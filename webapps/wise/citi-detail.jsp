<%@ include file="/commons/taglib.jsp"%>
<%@page language="java" contentType="application/json"  pageEncoding="UTF-8"
    import="org.apache.wink.json4j.*"
    import="com.fasterxml.jackson.databind.ObjectMapper"
    import="com.intumit.citi.backend.*"
    import="com.intumit.citi.frontend.*"
    import="com.intumit.citi.*"
    import="com.intumit.solr.robot.*"
    import="org.apache.wink.json4j.*"    
    import="org.apache.commons.lang.StringUtils"
    import="java.util.*"
    import="com.intumit.solr.tenant.*"
    import="java.net.*"
%><%!

private String formalAns(String key, Tenant tt)
{
    return RobotFormalAnswers.getAnswers(tt.getId(),key).get(0).toString();  
}

%><%

Tenant t = null;
if (request.getParameter("apikey") != null) {
    String apikey = request.getParameter("apikey");
    com.intumit.solr.tenant.Apikey k = com.intumit.solr.tenant.Apikey.getByApiKey(apikey);
    t = k != null ? k.getTenant() : null;//Tenant.getTenantByApiKey(apikey);
}

if (t == null) {
    JSONObject errorOutput = new JSONObject();
    errorOutput.put("errorCode", 400);
    errorOutput.put("errorMessage", "Cann't determine skill type.");

    out.println(errorOutput.toString(2));

    return;
}

System.out.println("http://twwsb-chatbot1u.apac.nsroot.net:9080/wise/citi-detail.jsp?apikey=" + request.getParameter("apikey")
+ "&id=" + URLEncoder.encode(request.getParameter("id"), "UTF-8") + "&cardno=" + request.getParameter("cardno") );

Menu menu;
ObjectMapper mapper;
Result result;

menu = new Menu();
mapper = new ObjectMapper();
result = new Result();
result.setCode(0);
result.setMessage("");
menu.setResult(result);
//TreeMap tm = new TreeMap(Collections.reverseOrder());
String qaId = request.getParameter("id");
//QAContext qaCtx = QAContextManager.lookup(qaId);
CardInfo cardinfo = CitiUtil.getSmartMenu(qaId, request.getParameter("cardno"), Result.Postfix.STMTDETAIL.toString()) ; //= ((CardInfo)qaCtx.getCtxAttr("cardinfo"));
if(cardinfo.getInfos() != null)
{
    BoxGrid box = new BoxGrid();
    box.setId(qaId);
    box.setType(Box.Type.GRID);
    box.setTitle(CitiUtil.currBillTranDetail);
    box.addHeader(new Header(formalAns("transDate", t),null)); //交易日
    box.addHeader(new Header(formalAns("incomeDate", t),null)); //入帳日
    box.addHeader(new Header(formalAns("goldAmount", t),"right")); //金額
    //Info info = cardinfo.getInfos().get(0); //.getAdditionalProperties(request.getParameter("carno"));
    int newLineCnt = 0;
    int tmInc = 0;
    String lastCard = "";
    GridRow row = null;
    for(Info info : cardinfo.getInfos())
    {
        for (Txn txn:info.getTxns()) 
        {
            if(StringUtils.equals(txn.getCardno(), lastCard) == false)
            {    
                row = new GridRow();
                row.setTitle(txn.getCardno().replaceFirst(".*(\\d{4})", ( StringUtils.equals( txn.getPrim(),"true" ) ? "主" : "附" ) + "卡 $1 新增消費"));
                row.setIsTitle(true);
                box.addRow(row);
                lastCard = txn.getCardno();
            }
            row = new GridRow();
            Field field = new Field();
            field.setText(txn.getTxndate());
            row.addField(field);
            field = new Field();
            field.setText(txn.getDate());
            row.addField(field);
            field = new Field();
            field.setText(txn.getDesc() + ( StringUtils.isNotEmpty(txn.getTxncurrencyen()) && txn.getTxncurrencyamount().matches(CitiUtil.isDouble) && Double.parseDouble(txn.getTxncurrencyamount())>0
                  ?"<span>(外幣本金 "+ txn.getTxncurrencyen() + CitiUtil.formatMoney(Double.parseDouble(txn.getTxncurrencyamount()), ": ") + " )</span>":""));
            row.addField(field);
            field = new Field();
            field.setText(txn.getAmount());
            row.addField(field);
            if (newLineCnt > 0)
                row.setIsAlternatingRow(true);
            newLineCnt++;
            box.addRow(row);
        }
        
        Rte rte = info.getRte();
        if(StringUtils.isNotEmpty(rte.getAmount()))
        {
            row = new GridRow();
            Field field = new Field();
            field.setText("");
            row.addField(field);
            field = new Field();
            field.setText("");
            row.addField(field);
            field = new Field();
            field.setText(formalAns("smartPay", t).replace("QQ", rte.getTenor())); //聰明付–本金<br/>聰明付-利息<br/>第QQ期
            row.addField(field);
            field = new Field();
            field.setText(rte.getAmount() + CitiUtil.newLine + rte.getInterest());
            row.addField(field);
            row.setIsAlternatingRow(true);
            box.addRow(row);
            newLineCnt++;
        }
    }
    menu.setBox(box);
}
String jsonInString = mapper/*.writerWithDefaultPrettyPrinter()*/.writeValueAsString(menu);
JSONObject json=null;
try {
    json = new JSONObject(jsonInString);
} catch (JSONException e) {
	  // TODO Auto-generated catch block
	  e.printStackTrace();
}

%><%= json.toString(4) %>