package com.intumit.citi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.hibernate.HibernateException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intumit.citi.backend.CardInfo;
import com.intumit.solr.admin.AdminGroup;
import com.intumit.solr.admin.AdminGroupFacade;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.tenant.Apikey;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeUtils;
import com.intumit.systemconfig.WiseSystemConfig;
import com.intumit.util.DesUtil;

public class CitiUtil {
    // 剛開始花旗信用卡的文字都放這裡，後來全轉去制式文案
    public static final String newLine = "<br/>";
    public static final String robotID = "";// "4eba966b-daa7-4f18-baf3-f8c1ebc9ddd3";
    public static final String sharingQuota = "";// "(共用額度)";
    public static final String singleQuota = "";// "(獨立額度)";
    public static final String points = "";// "點數" + newLine;
    public static final String bonus = "";// "紅利";
    public static final String jointName = "";// "聯名";
    public static final String usedQuata = "";// "已使用額度" + newLine;
    public static final String currentOverpayAmout = "";// "目前溢繳金額" + newLine;
    public static final String alreadyCancel = "";// "(已取消)";
    public static final String totalCredit = "";// "信用總額度" + newLine;
    public static final String availableCredit = "";// "可用額度" + newLine;
    public static final String totalAmountofCurrentBill = "";// "本期帳單總金額" +
                                                             // newLine;
    public static final String miniAmountPayment = "";// "最低應繳金額" + newLine;
    public static final String billCheckoutDate = "";// "帳單結帳日" + newLine;
    public static final String paymentDeadline = "";// "繳款截止日" + newLine;
    public static final String noPayment = "";// "無須繳款" + newLine;
    public static final String autoTransfer = "";// newLine + "自動轉帳";
    public static final String checkoutBill = "";// "結帳日" + newLine;
    public static final String emailBox = "";// "預計一小時內寄到您的信箱：" + newLine;
    public static final String currBillTranDetail = "";// "本期帳單交易明細";
    public static final String youMayWannaKnow = "";// "您可能還想知道：";
    public static final String userid = "";// "id";
    public static final String[] s1 = { "A", "B" }; // 花旗信用卡的 Block Code
    public static final String[] s2 = { "F", "L", "U", "W" };
    public static final String[] s3 = { "C", "D", "E", "M", "P", "S" };
    public static final String unTranDetail = "";// "https://www.citibank.com.tw/TWGCB/JSO/signon/DisplayUsernameSignon.do?ts=1545366080431";
    public static final String applyBilling = "";// "https://www.citibank.com.tw/global_docs/chi/cc/150702_rte/index.htm";
    public static final String payRightNow = "";// "https://www.citibank.com.tw/sim/zh-tw/cbol/efisc.htm";
    private static String myIP = "";
    public static final String poleBear = "";// "e起愛地球救救北極熊，註冊電子月結單讓你對帳最簡單。";
    private static Properties props;
    public static final String isNumeric = "-?(0|[1-9]\\d*)";
    public static final String isDouble = "-?(0|[1-9]\\d*)\\.([0-9]+)";

    private static final String ERROR_INFO_SPLITTER = "$";
    private static final Integer ERROR_INFO_EXPIRE_THRESHOLD = Integer.valueOf(10);
    private static final String CITI_TID = "citi";
    private static final String AI_SUPERVISOR = "AI Supervisor";
    private static final String AI_TRAINER = "AI Trainer"; 

    private static String DEV_PRO_DYNAMIC_PWD_URL;
    private static CitiDBInfo CURRENT_DB_INFO = CitiDBInfo.EMPTY;
    private static String DB_API_SENDER;
    private static String DB_API_SESSIONID;
    private static String DB_API_DATABASENAME;
    private static Integer DB_INFO_EXPIRED_THRESHOLD_IN_SECOND;

    static {
        try {
            Properties devproDynamicPwdProp = new Properties();   
            Properties prop = new Properties();

            try {
            	prop.load(new FileInputStream(Paths.get(System.getProperty("dynamicPwd")).toFile()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            devproDynamicPwdProp = prop;
            
            DEV_PRO_DYNAMIC_PWD_URL = devproDynamicPwdProp.getProperty("devproUrl");
            DB_API_SENDER = devproDynamicPwdProp.getProperty("sender");
            DB_API_SESSIONID = devproDynamicPwdProp.getProperty("sessionId");
            DB_API_DATABASENAME = devproDynamicPwdProp.getProperty("databaseName");
            
            DB_INFO_EXPIRED_THRESHOLD_IN_SECOND = NumberUtils
                    .toInt(devproDynamicPwdProp.getProperty("expireThreshold"), 600);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 當初想作一個使用 h2 DB 的 config 機制，後來單機使用 WiseSystemConfig 就沒繼續
     */
    private static void loadProperties() {
        File yourFile = new File("CitiConf.properties");
        try {
            yourFile.createNewFile();
            props = new Properties();
            props.load(new FileInputStream("CitiConf.properties"));// 設定檔案名稱
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getProperties(String key, String defaultValue) {// 把DatabaseConf.properties載入程式
        if (null == props) loadProperties();
        return props.getProperty(key, defaultValue);
    }

    public static String getMyLink() {
        String port = WiseSystemConfig.get().getLocalPort();
        String apikey = Apikey.get((long)1).getApikey();
        if (myIP.equals("")) {
            try (final DatagramSocket socket = new DatagramSocket()) {
                socket.connect(InetAddress.getByName("www.citibank.com.tw"), 80);
                myIP = "https://" + socket.getLocalAddress().getHostAddress() + ":" + port
                        + WiseSystemConfig.get().getContextPath() + "/qa-ajax.jsp?apikey=" + apikey;
            }
            catch (SocketException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                myIP = "localhost";
                e.printStackTrace();
            }
        }
        return getProperties("host", myIP);
    }

    public static String formatMoney(double doubleNum, String prefix) {
        String ret = NumberFormat.getNumberInstance().format(doubleNum);
        ret = prefix + "$" + ret;
        return ret;
    }

    public static String formatMoney(int intNum, String prefix) {
        String ret = NumberFormat.getNumberInstance().format(intNum);
        ret = prefix + "$" + ret;
        return ret;
    }

    public static String formatMoney(String strNum, fontColor myColor) {
        String ret = "$" + formatNum(strNum, myColor);
        // ret = formatColor(ret, myColor);
        return ret;
    }

    public static String formatNum(String strNum, fontColor myColor) {
        String ret = NumberFormat.getNumberInstance().format(Integer.valueOf(strNum));
        // ret = formatColor(ret, myColor);
        return ret;
    }

    public static String formatColor(String strColor, fontColor myColor) {
        strColor = strColor.replaceFirst("(.+)", myColor.toString());
        return strColor;
    }

    public static String formatDate(Date date, String format) {
        String ret = new SimpleDateFormat(format).format(date);
        // ret = formatColor(ret, myColor);
        return ret;
    }

    /**
     * 從一個JSonObject/JSonArray 累加拷貝至另一個 JSonObject/JSonArray, 後來專案沒用到
     * 
     * @param input
     * @param output
     */
    private static void loopThroughJson(Object input, Object output) {
        try {
            if (input instanceof JSONObject) {
                Iterator<?> keys = ((JSONObject)input).keys();
                while (keys.hasNext()) {
                    String key = (String)keys.next();
                    if (!(((JSONObject)input).get(key) instanceof JSONArray)) {
                        // System.out.println(key + "=" + ((JSONObject)
                        // input).get(key));
                        Object value = ((JSONObject)input).get(key);
                        if (null != value) ((JSONObject)output).put(key, value);
                    }
                    else {
                        Object jary;
                        if (((JSONObject)output).has(key)) {
                            jary = ((JSONObject)output).get(key);
                        }
                        else {
                            jary = new JSONArray();
                        }
                        ((JSONObject)output).put(key, jary);
                        loopThroughJson(new JSONArray(((JSONObject)input).get(key).toString()),
                                jary);
                    }
                }
            }
            if (input instanceof JSONArray) {
                for (int i = 0; i < ((JSONArray)input).length(); i++) {
                    JSONObject a = ((JSONArray)input).getJSONObject(i);
                    Object key = a.keys().next().toString();
                    if (!(a.opt(key.toString()) instanceof JSONArray)) {
                        // System.out.println(key + "=" +
                        // a.opt(key.toString()));
                        Object json;
                        if (((JSONArray)output).length() == ((JSONArray)input).length()) {
                            json = ((JSONArray)output).getJSONObject(i);
                        }
                        else {
                            json = new JSONObject();
                            ((JSONArray)output).add(json);
                        }
                        loopThroughJson(a, json);
                    }
                    else {
                        Object jary = ((JSONArray)output).getJSONObject(i);
                        Object obj = new JSONArray();
                        ((JSONObject)jary).put(key.toString(), obj);
                        loopThroughJson(a.opt(key.toString()), obj);
                    }
                }
            }
        }
        catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 從 NaverLineAnswerTransformer.getUserProfile 修改而來
     * 
     * @param lineData
     * @param postfix
     * @return
     */
    private static String HttpPost(Map<String, Object> lineData, String postfix) {
        String jsonResp = null; // JSONObject jsonResp = null;
        CloseableHttpClient httpClient = null;
        try {
            JSONObject json = new JSONObject();
            json.putAll(lineData);
            String backend = getProperties("url",
                    "https://devpro-chat.cloudapp.net/echatservice/mock/") + postfix;
            System.out.println(backend);
            if (backend.startsWith("https")) {
                // Create a trust manager that does not validate certificate
                // chains
                TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                } };

                // Install the all-trusting trust manager
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());

                httpClient = HttpClients.custom()
                        .setSSLSocketFactory(new SSLConnectionSocketFactory(sc.getSocketFactory(),
                                new X509HostnameVerifier() {
                                    @Override
                                    public boolean verify(String arg0, SSLSession arg1) {
                                        return true;
                                    }

                                    @Override
                                    public void verify(String arg0, SSLSocket arg1)
                                            throws IOException {}

                                    @Override
                                    public void verify(String arg0, X509Certificate arg1)
                                            throws SSLException {}

                                    @Override
                                    public void verify(String arg0, String[] arg1, String[] arg2)
                                            throws SSLException {}
                                }))
                        .build();
            }
            else httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(backend);
            StringEntity params = new StringEntity(json.toString());
            request.addHeader("content-type", "application/json");
            // request.addHeader("Authorization", "Bearer " + accessToken);
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);

            if (response != null) {
                System.out.println("StatusCode : " + response.getStatusLine().getStatusCode());

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent()));
                String inputLine;
                StringBuffer resultData = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    resultData.append(inputLine);
                }
                in.close();
                jsonResp = resultData.toString(); // new
                                                  // JSONObject(resultData.toString());
                System.out.println(resultData.toString());
            }
        }
        catch (Exception ex) {
            // handle exception here
            System.out.println(ex.toString());
        }
        finally {
            httpClient.getConnectionManager().shutdown();
            return jsonResp;
        }
    }

    /**
     * 花旗帳單明細用
     * 
     * @param myID
     *            花旗 token
     * @param cardno
     *            信用卡號
     * @param postfix
     *            帳單明細 API
     * @return
     */
    public static CardInfo getSmartMenu(String myID, String cardno, String postfix) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> lineData = new HashMap<String, Object>();
        lineData.put("token", myID);
        lineData.put("cardno", cardno);
        String jsonInString = CitiUtil.HttpPost(lineData, postfix);
        try {
            CardInfo cardinfo = mapper.readValue(jsonInString, CardInfo.class);
            return cardinfo;
        }
        catch (JsonParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param myID
     *            花旗 token
     * @param postfix
     *            花旗的五種 API, 我的卡片、帳單資訊、補寄帳單、卡片權益、繳款紀錄
     * @return
     */
    public static CardInfo getSmartMenu(String myID, String postfix) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> lineData = new HashMap<String, Object>();
        lineData.put("token", myID);
        String jsonInString = CitiUtil.HttpPost(lineData, postfix);
        try {
            CardInfo cardinfo = mapper.readValue(jsonInString, CardInfo.class);
            return cardinfo;
        }
        catch (JsonParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 一開始與迪普文字客服串接時所測是用，後來轉至花旗UAT就沒再用了
     * 
     * @param myID
     * @return
     */
    public static CardInfo getCardInfo(String myID) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> lineData = new HashMap<String, Object>();
        lineData.put("token", myID);
        String jsonInString = CitiUtil.HttpPost(lineData, Result.Postfix.CARDINFO.toString());
        try {
            JSONObject walk = new JSONObject();
            CitiUtil.loopThroughJson(new JSONObject(jsonInString), walk);
            CardInfo cardinfo = mapper.readValue(jsonInString, CardInfo.class);
            // Iterator<?> keys = cardinfo.getInfos().iterator();
            // StringBuilder sb = new StringBuilder("[");
            // while (keys.hasNext()) {
            // sb.append(((Info)keys.next()).getCardno()).append(",");
            // }
            // sb.deleteCharAt(sb.length()-1).append("]");
            jsonInString = CitiUtil.HttpPost(lineData, Result.Postfix.STATEMENT.toString());
            CitiUtil.loopThroughJson(new JSONObject(jsonInString), walk);
            jsonInString = CitiUtil.HttpPost(lineData, Result.Postfix.STMTDETAIL.toString());
            CitiUtil.loopThroughJson(new JSONObject(jsonInString), walk);
            jsonInString = CitiUtil.HttpPost(lineData, Result.Postfix.PYMRECORD.toString());
            CitiUtil.loopThroughJson(new JSONObject(jsonInString), walk);
            jsonInString = CitiUtil.HttpPost(lineData, Result.Postfix.RESENDESTMT.toString());
            CitiUtil.loopThroughJson(new JSONObject(jsonInString), walk);
            cardinfo = mapper.readValue(walk.toString(), CardInfo.class);
            return cardinfo;
        }
        catch (JsonParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 後來內文顏色全由迪普處理，文字顏色 enum 沒繼續用
     * 
     * @author user
     */
    public enum fontColor {

        BLUE("<font color='blue' size='4'>$1</font>"), RED("<font color='red' size='4'>$1</font>");

        private final String value;
        private final static Map<String, CitiUtil.fontColor> CONSTANTS = new HashMap<String, CitiUtil.fontColor>();

        static {
            for (CitiUtil.fontColor c : values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private fontColor(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static CitiUtil.fontColor fromValue(String value) {
            CitiUtil.fontColor constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            }
            else {
                return constant;
            }
        }

    }

    public static Optional<AdminUser> getAndUpdateAdminUser(String loginName)
            throws SsoErrorException {
        String url = StringUtils.defaultIfBlank(WiseSystemConfig.get().getSsoPermissionCheckUrl(),
                "https://devpro.southeastasia.cloudapp.azure.com/echatservice/mock/AgentTeam");

        try {
            JSONObject data = new JSONObject();
            data.put("tid", CITI_TID);
            data.put("aid", loginName);
            String devProResp = WiSeUtils.postJson(url, data, "UTF-8");
            if (StringUtils.isBlank(devProResp)) {
                throw new SsoErrorException(SsoErrorType.devProError);
            }
            // 迪普api response content type為application/json, 但很怪異地是回傳plain
            // text，所以用爛招做字串處理
            String devProRespPermission = StringUtils
                    .trimToEmpty(StringUtils.replace(devProResp, "\"", ""));
            return updateOrCreateUser(loginName, devProRespPermission);
        }
        catch (JSONException | HibernateException e) {
            e.printStackTrace();
            throw new SsoErrorException(SsoErrorType.robotError);
        }
    }

    private static Optional<AdminUser> updateOrCreateUser(String loginName, String permission) {
        AdminUserFacade userFacade = AdminUserFacade.getInstance();
        AdminUser user = userFacade.getByLoginName(loginName);
        if (StringUtils.isNotBlank(permission)) {
            AdminGroup targetGroup = getOrCreateGroup(permission);
            if (user == null) {
                user = new AdminUser();
                user.setLoginName(loginName);
                user.setName(loginName);
                user.setPassword("");
                user.setAdminGroups(String.valueOf(targetGroup.getId()));
                user.setTenantIds(Tenant.list().stream().map(t -> String.valueOf(t.getId()))
                        .collect(Collectors.joining(",")));
                userFacade.saveOrUpdate(user);
            }
            else {
                AdminGroup notTargetGroup = getOrCreateGroup(
                        StringUtils.equalsIgnoreCase(permission, AI_SUPERVISOR)
                                ? AI_TRAINER
                                : AI_SUPERVISOR);
                Set<String> userGroupIds = new HashSet<>();
                Arrays.asList(user.getAdminGroups().split(",")).forEach(g -> userGroupIds.add(g));
                userGroupIds.remove(String.valueOf(notTargetGroup.getId()));
                userGroupIds.add(String.valueOf(targetGroup.getId()));
                user.setAdminGroups(StringUtils.join(userGroupIds, ","));
                user.setPassword("");
                user.setDisabled(false);
                userFacade.saveOrUpdate(user);
            }
            return Optional.of(user);
        }
        else {
            if (user != null) {
                user.setDisabled(true);
                userFacade.saveOrUpdate(user);
            }
            return Optional.empty();
        }
    }

    public static String getEncryptedSsoError(String errorType, String loginName) {
        try {
            StringBuffer combineStr = new StringBuffer(errorType);
            combineStr.append(ERROR_INFO_SPLITTER);
            combineStr.append(loginName);
            combineStr.append(ERROR_INFO_SPLITTER);
            combineStr.append(Instant.now().getEpochSecond());
            return URLEncoder.encode(DesUtil.encrypt(combineStr.toString()), "UTF-8");
        }
        catch (UnsupportedEncodingException ignore) {
            throw new RuntimeException(ignore);
        }
    }

    public static String getSsoErrorMessage(Locale locale, String encryptedMsg) {
        if (StringUtils.isBlank(encryptedMsg)) return "";
        try {
            String decrypted = DesUtil.decrypt(encryptedMsg);
            String[] errorMsgInfos = StringUtils.split(decrypted, ERROR_INFO_SPLITTER);
            if (errorMsgInfos.length == 3) {
                String errorTypeOrMsg = errorMsgInfos[0];
                String loginName = errorMsgInfos[1];
                String timeInSec = errorMsgInfos[2];
                if ((Instant.now().getEpochSecond()
                        - Long.parseLong(timeInSec)) <= ERROR_INFO_EXPIRE_THRESHOLD) {
                    try {
                        SsoErrorType type = SsoErrorType.fromName(errorTypeOrMsg);
                        if (type != null) {
                            return type.getMessage(locale, loginName);
                        }
                        return errorTypeOrMsg;
                    }
                    catch (Exception ignore) {
                        ignore.printStackTrace();
                    }
                }
            }
        }
        catch (Exception ignore) {
            ignore.printStackTrace();
        }
        return "";
    }

    private static AdminGroup getOrCreateGroup(String groupName) {
        AdminGroupFacade groupFacade = AdminGroupFacade.getInstance();
        AdminGroup target = groupFacade.getByName(groupName);
        if (target == null) {
            target = new AdminGroup();
            target.setName(groupName);
            target.setSystemAdminCURD(AdminGroup.SUPER_GROUP_PERM);
            if (StringUtils.equalsIgnoreCase(AI_SUPERVISOR, groupName)) {
                target.setAdminAdminCURD(
                        AdminGroup.C | AdminGroup.D | AdminGroup.R | AdminGroup.E1);
                target.setIndexAdminCURD(
                        AdminGroup.C | AdminGroup.D | AdminGroup.R | AdminGroup.E1);
                target.setCoreAdminCURD(AdminGroup.C | AdminGroup.D | AdminGroup.R | AdminGroup.E1);
                target.setSystemAdminCURD(AdminGroup.SUPER_GROUP_PERM);
                target.setDataSetAdminCURD(AdminGroup.SUPER_GROUP_PERM);
                target.setStatisticsAdminCURD(
                        AdminGroup.C | AdminGroup.D | AdminGroup.R | AdminGroup.E1);
            }
            groupFacade.saveOrUpdate(target);
        }
        return target;
    }

    public static CitiDBInfoApiResultDto getCitiDBInfoResult(String dbName) {
//        synchronized (MUTEX) {
            try {
                if (CURRENT_DB_INFO.isExpired(DB_INFO_EXPIRED_THRESHOLD_IN_SECOND)) {
                    JSONObject params = new JSONObject();
                    params.put("sender", DB_API_SENDER);
                    params.put("sessionId", DB_API_SESSIONID);
                    JSONArray dbNames = new JSONArray();
                    dbNames.add(DB_API_DATABASENAME);
                    dbName = DB_API_DATABASENAME;
                    params.put("databaseName", dbNames);
                    String devproResp = "";//WiSeUtils.postJson(DEV_PRO_DYNAMIC_PWD_URL, params, "UTF-8");
                    if (StringUtils.isNotBlank(devproResp)) {
                        JSONObject devpro = new JSONObject(devproResp);
                        JSONArray contents = devpro.optJSONArray("contents");
                        if (contents.length() > 0) {
                            JSONObject dbInfo = contents.getJSONObject(0);
                            CitiDBInfo refreshOne = new CitiDBInfo.Builder()
                                    .setAccount(StringUtils.trim(dbInfo.optString("userName")))
                                    .setAddress(StringUtils.trim(dbInfo.optString("address")))
                                    .setPwd(StringUtils.trim(dbInfo.optString("secureCode")))
                                    .setDbName(dbName)
                                    .setUpdateTime(System.currentTimeMillis()).build();
                            CitiDBInfoApiResultDto result = CitiDBInfoApiResultDto.BAD_RESULT;
                            synchronized (CURRENT_DB_INFO) {
                                result = new CitiDBInfoApiResultDto(CURRENT_DB_INFO,
                                        refreshOne);
                                if (result.isNeedInit()) {
                                    CURRENT_DB_INFO = refreshOne;
                                }
                                else {
                                    CURRENT_DB_INFO.refreshUpdateTime();
                                }
                            }
                            
                            System.out.println("__________________");
                            System.out.println("********userName: " + dbInfo.optString("userName"));
                            System.out.println("********userPwd: " + dbInfo.optString("secureCode"));
                            System.out.println("********address: " + dbInfo.optString("address"));
                            System.out.println("********dbName: " + dbName);
                            System.out.println("__________________");
                            
                            return result;
                        }
                    }     
                }
            }
            catch (Exception ignore) {
                ignore.printStackTrace();
            }
//        }
        // 有異常回傳舊的
        return new CitiDBInfoApiResultDto(CURRENT_DB_INFO, CURRENT_DB_INFO);
    }

    public static CitiDBInfo getCurrentInited(String dbName) {
        if (CURRENT_DB_INFO.equals(CitiDBInfo.EMPTY)) {
            getCitiDBInfoResult(dbName);
        }
        return CURRENT_DB_INFO;
    }
    
}