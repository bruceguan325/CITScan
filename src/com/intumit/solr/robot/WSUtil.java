package com.intumit.solr.robot;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.ws.Service;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import tw.com.cathaybk.webservice.client.fep.FEPService;

import com.ibm.icu.text.SimpleDateFormat;
import com.intumit.solr.robot.DeviceBinding.Status;
import com.intumit.systemconfig.WiseSystemConfigFacade;
import com.jamesmurty.utils.XMLBuilder2;

public class WSUtil {
	
	public static final String DEFAULT_BANCS_ENDPOINT = "http://dar6midd01:38080/WEBS/services/BANCSService";
	public static final String DEFAULT_CARD_ENDPOINT = "http://dar6midd01:38070/WEBS/services/CARDService";
	public static final String DEFAULT_FEP_ENDPOINT = "http://dar6midd01:38080/WEBS/services/FEPService";
	
	public static final String WS_NAMESPACE = "http://webservice.cathaybk.com.tw";
	
	static final String BANCS_SERVICE = "BANCSServiceService";
	static final String CARD_SERVICE = "CARDServiceService";
	static final String FEP_SERVICE = "FEPServiceService";
	
	public enum WSService {
		
		CardBalance("CRDCTIC0070", CARD_SERVICE),
		CardAccountAmount("CRDCTIC0070", CARD_SERVICE),
		CardPoint("CRDCTIC0570", CARD_SERVICE),
		AccountBalance("FNSCIF0062", BANCS_SERVICE),
		Pin("FPIN1001", FEP_SERVICE);
		
		public final String code;
		
		public final String name;
		
		WSService(String code, String name){
			this.code = code;
			this.name = name;
		}
		
		public String getNamespace(){
			return "http://www.cathaybk.com.tw/webservice/" + code + "/";
		}
		
	}
	
	public static class CrdBalance {
		/** 本期應繳款 */
		public String currentBalance;
		/** 帳單日 */
		public String statementDate;
		/** 繳款截止日 */
		public String paymentDueDate;
		/** 最低應繳款  */
		public String dueAmount;
		
		public String getMsg(int year, int month){
			String msg = (year - 1911) + "年 " + (month < 10?"0":"") + month + "月信用卡對帳單"
				+ "<br/>・帳單結帳日：" + statementDate
				+ "<br/>・繳款截止日：" + paymentDueDate
				+ "<br/>・本期應繳總額：NTD " + currentBalance
				+ "<br/>・本期最低應繳總額：NTD " + dueAmount;
			return msg;
		}
	}
	
	public static class CrdAmount {
		/** 帳戶餘額 */
		public String accountAmount;
		/** 信用額度 */
		public String creditLine;
		/** 國內預現額度 */
		public String cashLineInterest;
		/** 本期預借現金 */
		public String cashAdvAmount;
		
		public String getMsg(){
			String msg = "可用額度：NTD " + accountAmount
				+ "<br/>*****************************"
				+ "<br/>・永久信用額度：NTD " + creditLine
				+ "<br/>・國內預借現金額度：NTD " + cashLineInterest
				//+ "<br/>・國外預借現金額度： NTD 180,000"
				+ "<br/>・已消費/預借現金總額：NTD " + cashAdvAmount
				+ "<br/><br/>【說明】"
				+ "<br/>國內/國外預借現金額度合併計算並佔您的信用額度。";
			return msg;
		}
	}
	
	public static class CrdPoint {
		/** 帳單日可用點數 */
		public String pointRest;
		/** 目前可用點數 */
		public String currPoint;
		
		public String getMsg(){
			String msg = "・目前可用的點數：" + currPoint
				+ "<br/>・帳單日可用點數：" + pointRest
				+ "<br/><br/>【說明】"
				+ "<br/>此處所列示的紅利點數資料僅供您參考。實際累積的紅利點數，請以帳單為準。";
			return msg;
		}
		
		public boolean hasData(){
			return StringUtils.isNotBlank(pointRest) && StringUtils.isNotBlank(currPoint);
		}
	}
			
	public static class AccBalance {
		/** 台幣餘額 */
		public String twd = "";
		/** 可用餘額(含透支) */
		public String available = "";
		
		public String getMsg(){
			String msg = "您好，台幣帳戶餘額查詢結果如下："
				+ "<br/>・帳戶餘額：NTD " + twd
				+ "<br/>・可用餘額：NTD " + available
				+ "<br/><br/>【說明】"
				+ "<br/>1. 「帳戶餘額」包括本交金額、ｉ刷金融卡簽帳消費但特約商店尚未請款之金額與圈存金額，不含透支金額。 (使用ｉ刷金融卡簽帳消費但特約商店尚未請款之金額，該款項之權利於簽帳消費之同時即已移轉予特約商店，故您無法提領該筆款項。)"
				+ "<br/>2. 「可用餘額」包含透支金額，不含本交金額、ｉ刷金融卡簽帳消費但特約商店尚未請款之金額與圈存金額。";
			return msg;
		}
	}
	
	public static class Pin {
		
		static final String FUND_ACTION_NAME = "SpecialSet/P0412_AICustServiceSet";
		
		public static String getMsgInvite(String deviceId){
			String loginUrl = WiseSystemConfigFacade.getInstance().get().getiBankLoginUrl();
			if(StringUtils.isNotBlank(deviceId)){
				String prefix = WiseSystemConfigFacade.getInstance().get()
					.getAppIbankUrlTransformPrefix();
				try {
					loginUrl = prefix + URLEncoder.encode(UrlTransformer.base64Encode(FUND_ACTION_NAME), "utf-8");
				} catch (UnsupportedEncodingException e) {}				
			}
			return "您好，現在立即登入行動銀行認證您的行動裝置，即可享受以下快速查詢服務："
				+ "<br/>・台幣帳戶餘額查詢"
				+ "<br/>・當期信用卡帳單查詢"
				+ "<br/>・信用卡可用餘額查詢"
				+ "<br/>・信用卡紅利查詢"
				+ "<br/><br/><a href='" + StringEscapeUtils.escapeHtml(loginUrl) + "'>按這裡，立即認證行動裝置</a>";
		}
		
		public String nid;
		/**  
		 * 密碼使用等級
		 * “A”可使用所有通路及功能
		 * “C”C級用戶，限使用信用卡功能
		 * */
		public String accessLevel;
		/** 
		 * 密碼狀態
		 * 0-未申請
		 * 1-已申請覆核
		 * 2-已生效→僅此狀態可以顯示查詢結果
		 * 3-註銷(停用)
		 * 4-失效(密碼錯誤４次鎖住)
		 * */
		public String pinStatus;
		
		public boolean isLevelA(){
			return "A".equals(StringUtils.trim(accessLevel));
		}
		
		public boolean isActivated(){
			return "2".equals(StringUtils.trim(pinStatus));
		}
		
		public String getStatusMsg(){
			String msg = "";
			if("3".equals(StringUtils.trim(pinStatus))){
				msg = "您好，因為您的理財密碼已註銷停用，欲重新申請可透過以下管道"
					+ "<br/>(1)【存款戶】請本人攜帶身分證及原留印鑑至分行臨櫃申請"
					+ "<br/>(2) 【信用卡戶】請致電客服專線02-2383-1000(手機請撥市話)或0800-818-001線上申請";
			}else if("4".equals(StringUtils.trim(pinStatus))){
				msg = "您好，因為您的理財密碼已失效，欲重新申請可透過以下管道"
					+ "<br/>【存款戶】"    
					+ "<br/>(1)您可持本行晶片金融卡及於本行留存號碼之手機至國泰世華全台ATM進行重設(僅限觸控式螢幕機台適用)。"
					+ "<br/>(2)登入本行網路ATM (MyATM)進行重設(需搭配本行晶片金融卡、晶片讀卡機及於本行留存號碼之手機)。"
					+ "<br/>(3)於營業時間攜帶您的身分證正本及原留印鑑親至任一分行申請重設。"
					+ "<br/><br/>【信用卡戶】"
					+ "<br/>2.忘記「信用卡理財密碼」(僅有信用卡查詢功能) ，請洽客服專線02-2383-1000(手機請撥市話)或0800-818-001申請。";
			}
			return msg;
		}
	}
	
	static final String SOURCECHANNEL = "OTR-LX-ICS-01";
	static final String AGENT_ID = "";
	static final String ID_TYPE = "11";
	static final String CUSTOMER_NUMBER = "00000000000000000";
	static final String CLOSE_ACCT_INCLUDE = "N"; 
	static final String BANCS_APPLICATION = "DEP"; 
	static final String BRANCH_NO = "0000"; 
	static final String PRODUCT_TYPE = "0000"; 
	static final String PRODUCT_SUB_TYPE = "0000"; 
	
	/** CRDCTIC0070 */
	public static String toCardBalanceReqXml(String idNo, int year, int month){
		int statementDate = Integer.parseInt(toXmlVal(4, year) + toXmlVal(2,month));
		XMLBuilder2 xml = createReqXml(WSService.CardBalance)
			.e("TRANRQ")
				.e("AgentId").t(toXmlVal(8, AGENT_ID)).up()
				.e("StatementDate").t(toXmlVal(6, statementDate)).up()
				.e("IdNo").t(toXmlVal(12, idNo)).up();
		return toString(xml);
	}
	
	/** CRDCTIC0570 */
	public static String toCardPointReqXml(String idNo){
		XMLBuilder2 xml = createReqXml(WSService.CardPoint)
			.e("TRANRQ")
				.e("IdNo").t(toXmlVal(12, idNo)).up()
				.e("InquiryKey").t(toXmlVal(35, "")).up();
		return toString(xml);
	}
	
	/** FNSCIF0062 */
	public static String toAccountBalanceReqXml(String idNo){
		XMLBuilder2 xml = createReqXml(WSService.AccountBalance)
			.e("TRANRQ")
				.e("BranchId").t(toXmlVal(4, 9999)).up()
				.e("TellerId").t(toXmlVal(5, 0)).up()
				.e("FnsFlagX").t(toXmlVal(1, "")).up()
				.e("IdType").t(toXmlVal(2, ID_TYPE)).up()
				.e("IdNo").t(toXmlVal(14, idNo)).up()
				.e("CustomerNumber").t(toXmlVal(17, CUSTOMER_NUMBER)).up()
				.e("CloseAcctInclude").t(toXmlVal(1, CLOSE_ACCT_INCLUDE)).up()
				.e("InquiryKey").t(toXmlVal(50, "")).up()
				.e("BancsApplication").t(toXmlVal(3, BANCS_APPLICATION)).up()
				.e("BranchNo").t(toXmlVal(4, BRANCH_NO)).up()
				.e("ProductType").t(toXmlVal(4, PRODUCT_TYPE)).up()
				.e("ProductSubType").t(toXmlVal(4, PRODUCT_SUB_TYPE)).up();
		return toString(xml);
	}
	
	/** FPIN1001 */
	public static String toPinReqXml(String idNo){
		XMLBuilder2 xml = createReqXml(WSService.Pin)
			.e("TRANRQ")
				.e("SourceChannelId").t(toXmlVal(13, SOURCECHANNEL)).up()
				.e("CustomerId").t(toXmlVal(12, idNo)).up();
		return toString(xml);
	}
	
	/** CRDCTIC0070本期帳單金額 */
	public static CrdBalance parseCardBalance(String xml){
		CrdBalance cb = new CrdBalance();
		Document xmlDoc = parseXml(xml);
		cb.currentBalance = formatCurrency(fromXmlNumVal(xmlDoc.select("tranrs > currentbalance").text()));
		cb.dueAmount = formatCurrency(fromXmlNumVal(xmlDoc.select("tranrs > dueamount").text()));
		cb.paymentDueDate = xmlDoc.select("tranrs > paymentduedate").text();
		cb.statementDate = xmlDoc.select("tranrs > statementdate").text();
		return cb;
	}
	
	/** CRDCTIC0070信用卡可用餘額 */
	public static CrdAmount parseCardAccountAmount(String xml){
		CrdAmount ca = new CrdAmount();
		Document xmlDoc = parseXml(xml);
		ca.accountAmount = formatCurrency(fromXmlNumVal(xmlDoc.select("tranrs > accountamount").text()));
		ca.cashAdvAmount = formatCurrency(fromXmlNumVal(xmlDoc.select("tranrs > cashadvamount").text()));
		ca.cashLineInterest = formatCurrency(fromXmlNumVal(xmlDoc.select("tranrs > cashlineinterest").text()));
		ca.creditLine = formatCurrency(fromXmlNumVal(xmlDoc.select("tranrs > creditline").text()));
		return ca;
	}
	
	/** CRDCTIC0570累積紅利點數 */
	public static CrdPoint parseCardPoint(String xml){
		CrdPoint cp = new CrdPoint();
		Document xmlDoc = parseXml(xml);
		cp.currPoint = formatCurrency(fromXmlNumVal(xmlDoc.select("tranrs > currpoint").text()));
		cp.pointRest = formatCurrency(fromXmlNumVal(xmlDoc.select("tranrs > pointrest").text()));
		return cp;
	}
	
	/** FNSCIF0062帳戶餘額 */
	public static AccBalance parseAccountBalance(String xml){
		AccBalance ab = new AccBalance();
		Document xmlDoc = parseXml(xml);
		ab.twd = formatCurrency(fromXmlNumVal(xmlDoc.select("tranrs > records > record:eq(0) > twdbalance").text()));
		ab.available = formatCurrency(fromXmlNumVal(xmlDoc.select("tranrs > records > record:eq(0) > availablebalance").text()));
		return ab;
	}
	
	/** FPIN1001理財密碼資料查詢 */
	public static Pin parsePin(String xml){
		Pin p = new Pin();
		Document xmlDoc = parseXml(xml);
		p.accessLevel = xmlDoc.select("tranrs > accesslevel").text();
		p.pinStatus = xmlDoc.select("tranrs > pinstatus").text();
		return p;
	}
	
	public static <T> T getClient(String url, String service, String namespace,
			Class<T> endpoint) throws MalformedURLException {
		QName qname = new QName(namespace, service);
		Service s = Service.create(new URL(url), qname);
		return s.getPort(endpoint);
	}
	
	public static <T> T getClient(String url, WSService service,
			Class<T> endpoint) throws MalformedURLException {
		QName qname = new QName(WS_NAMESPACE, service.name);
		Service s = Service.create(new URL(url), qname);
		return s.getPort(endpoint);
	}
	
	public static Pin getPin(String deviceToken){
		Pin pin = null;
		String nid = getNIDFromDeviceBinding(deviceToken);
		if(nid != null){
			try{
				String url = WiseSystemConfigFacade.getInstance().get().getFepWsUrl();
				FEPService client = getClient(url, WSService.Pin, FEPService.class);
				String reqXml = toPinReqXml(nid);
				String resXml = client.xServiceMethod(reqXml);
				pin = parsePin(resXml);
			}catch(Exception e){
				pin = new Pin();
				e.printStackTrace();
			}
			pin.nid = nid;
		}
		return pin;
	}
	
	static Document parseXml(String xml){
		return Jsoup.parse(xml, "", Parser.xmlParser());
	}
	
	static XMLBuilder2 createReqXml(WSService endpoint){
		return XMLBuilder2.create("CUBXML", endpoint.getNamespace())
			.e("MWHEADER")
				.e("MSGID").t(endpoint.code).up()
				.e("SOURCECHANNEL").t(toXmlVal(13, SOURCECHANNEL)).up()
				.e("RETURNCODE").t(toXmlVal(4, "")).up()
				.e("RETURNDESC").t(toXmlVal(128, "")).up()
				.e("RETURNCODECHANNEL").t(toXmlVal(4, "")).up()
				.e("TXNSEQ").t(genTXNSEQ()).up()
				.e("O360SEQ").t(toXmlVal(20, "")).up()
			.up();
	}
	
	/** 文字型態靠左右補空白 */
	static String toXmlVal(int size, String val){
		return truncateStr(StringUtils.rightPad(StringUtils.defaultString(val), size), size);
	}
	
	/** 數字型態靠右左補0 */
	static String toXmlVal(int size, Number val){
		return truncateStr(StringUtils.leftPad(val.toString(), size, "0"), size);
	}
	
	static String fromXmlNumVal(String val){
		return StringUtils.defaultIfBlank(val, "0").replaceFirst("^\\+?0+(?!$)", "")
			.replaceFirst("\\+$", "");
	}
	
	static String truncateStr(String val, int maxSize){
		return val.substring(0, Math.min(maxSize, val.length()));
	}
	
	/** 
	 * 前端系統自訂序號，作用為前端系統與MW勾連狀態為交易狀況查詢用的欄位，以利後續追蹤； 
	 * TR將此SOURCE_CHANNEL+ 此欄位為一unique key。
	 * 外圍系統如無編號機制，
	 * 建議格式可為(MMDDHHMMSSNN，NN為毫秒取前兩位)或（MMDD99999999），
	 * 惟合理性請依各系統自行考量。 
	 **/
	static String genTXNSEQ(){
		return new SimpleDateFormat("MMddHHmmssSS").format(new Date());
	}
	
	static String toString(XMLBuilder2 xml){
		Properties props = new Properties();
		props.put(OutputKeys.INDENT, "yes");
		props.put("{http://xml.apache.org/xslt}indent-amount", "2");
		props.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
		String out = "<?xml version=\"1.0\" encoding=\"BIG5\"?>\n" + xml.asString(props);
		return out;
	}
	
	static String getNIDFromDeviceBinding(String deviceToken){
		String nid = null;
		try{
			DeviceBinding binding = DeviceBinding.getByDeviceToken(deviceToken);
			if(binding != null && binding.getStatus() != Status.deleted){
				nid = binding.getNid();
			}
		}catch(Exception e){}
		return nid;
	}
	
	static String formatCurrency(String val){
		String out = "";
		DecimalFormat formatter = new DecimalFormat("#,###");
		try{
			out = formatter.format(Long.parseLong(val));
		}catch(NumberFormatException e){}
		return out;
	}
	
}
