package com.intumit.citi;

import java.net.*;
import java.util.*;

import org.junit.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intumit.citi.backend.*;
import com.intumit.citi.frontend.*;

public class Jackson2ExampleTest {

    Menu menu;
    ObjectMapper mapper;
    CardInfo cardinfo;
    
    @Before
    public void setUp() throws Exception {
        menu = new Menu();
        mapper = new ObjectMapper();
        cardinfo = CitiUtil.getCardInfo("A123456789");
        Result result = new Result();
        result.setCode(cardinfo.getResult().getCode());
        result.setMessage(cardinfo.getResult().getMessage());
        menu.setResult(result);
        
    }

    @After
    public void tearDown() throws Exception {
        String jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(menu);
        System.out.println(jsonInString);
    }

    private MessageText newMsgText(String robotID, String myText) {
        MessageText msgtxt = new MessageText();
        msgtxt.setId(robotID);
        msgtxt.setType(Message.Type.TEXT);
        msgtxt.setText(myText);
        return msgtxt;
    }
    
    private Content newContent(Content.Type type, String text) {
        Content content = new Content();
        content.setType(type);
        content.setText(text);
        return content;
    }
    
    private Action newAction(Action.Type type, String text, String url)
    {
    	Action action = new Action();
        action.setText(text);
        action.setType(Action.Type.URL);
        try {
            action.setUrl(url);
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return action;
    }
   
    @Test
    public void myCard001() {

        try {
            menu.addMessage(newMsgText(CitiUtil.robotID, "好的，這是您名下的卡片資訊。"));
        
            MessageCarousel msgcrl = new MessageCarousel();
            msgcrl.setId(CitiUtil.robotID);
            msgcrl.setType(Message.Type.CAROUSEL);
            
            List<Info> infos = cardinfo.getInfos();
            for (Info info:infos) {
                Column column = new Column();
                column.setImageUrl("https://pic.pimg.tw/chochoc/1527921496-567262214.jpg");
                column.setImageText(info.getCardno().replaceFirst(".*(\\d{4})", "xxx$1"));
                column.setTitle("PChome Prime 聯名御璽悠遊卡" + (info.getCcl().equals("N") ? CitiUtil.sharingQuota:"" ));
                column.addContent( newContent( Content.Type.TEXT, CitiUtil.billCheckoutDate + info.getStmtday() //CitiUtil.formatDate( info.getStmtday(), "yyyy/MM/dd" ) 
                                 ) );
                column.addContent( newContent( Content.Type.TEXT, ( info.getCcl().equals("Y") ? CitiUtil.bonus: CitiUtil.jointName ) +  
                	                           CitiUtil.points + CitiUtil.formatNum( info.getAvlPoint(), CitiUtil.fontColor.BLUE ) ) );
                column.addContent( newContent( Content.Type.TEXT, CitiUtil.usedQuata + CitiUtil.formatMoney(info.getCrL(), CitiUtil.fontColor.BLUE) ) );
                column.addContent( newContent( Content.Type.TEXT, CitiUtil.totalCredit + CitiUtil.formatMoney(info.getCurrBal(), CitiUtil.fontColor.BLUE) ) );
                column.addContent( newContent( Content.Type.TEXT, CitiUtil.availableCredit + CitiUtil.formatMoney(info.getAvlCredit(), CitiUtil.fontColor.BLUE) ) );
                msgcrl.addColumn(column);
            }
            menu.addMessage(msgcrl);
            
            MessageButtons msgbtn = new MessageButtons();
            msgbtn.setId(CitiUtil.robotID);
            msgbtn.setType(Message.Type.BUTTONS);
            msgbtn.setText("您可能還想知道：");
            msgbtn.addAction(newAction(Action.Type.URL,"如何申請調高永久額度？","https://www.citibank.com.tw/sim/zh-tw/loans/index.htm"));
            msgbtn.addAction(newAction(Action.Type.URL,"如何申請調高臨時額度？","https://www.citibank.com.tw/sim/zh-tw/loans/index.htm"));
            msgbtn.addAction(newAction(Action.Type.URL,"我最近一期帳單最晚甚麼時候要繳款？","https://www.citibank.com.tw/sim/zh-tw/loans/index.htm"));
            msgbtn.addAction(newAction(Action.Type.URL,"如何辨別信用卡有效期限與識別碼？","https://www.citibank.com.tw/sim/zh-tw/loans/index.htm"));
            menu.addMessage(msgbtn);            
            
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    
    @Test
    public void BillInfo002() {

    	try {
            menu.addMessage(newMsgText(CitiUtil.robotID, "好的，這是您名下所有信用卡的的帳務資訊。"));
        
            MessageCarousel msgcrl = new MessageCarousel();
            msgcrl.setId(CitiUtil.robotID);
            msgcrl.setType(Message.Type.CAROUSEL);
            
            List<Info> infos = cardinfo.getInfos();
            for (Info info:infos) {
                Column column = new Column();
                column.setImageUrl("https://pic.pimg.tw/chochoc/1527921496-567262214.jpg");
                column.setImageText(info.getCardno().replaceFirst(".*(\\d{4})", "xxx$1"));
                column.setTitle("PChome Prime 聯名御璽悠遊卡" + (info.getCcl().equals("N") ? CitiUtil.sharingQuota:"" ));
                column.addContent( newContent( Content.Type.TEXT, CitiUtil.totalAmountofCurrentBill + 
                		           CitiUtil.formatMoney( info.getEndBal(), CitiUtil.fontColor.BLUE ) ) );
                column.addContent( newContent( Content.Type.TEXT, CitiUtil.miniAmountPayment + CitiUtil.formatMoney(info.getTotAmtDue(), CitiUtil.fontColor.BLUE) ) );
                column.addContent( newContent( Content.Type.TEXT, CitiUtil.billCheckoutDate + info.getStmtday() //CitiUtil.formatDate( info.getStmtday(), "yyyy/MM/dd" ) 
                                 ) );
                column.addContent( newContent( Content.Type.TEXT, CitiUtil.paymentDeadline + 
                		            info.getDueDt() + CitiUtil.autoTransfer) );
                column.addExternalActions(newAction(Action.Type.URL,"未出帳交易明細","https://www.citibank.com.tw/sim/zh-tw/loans/index.htm"));
                column.addExternalActions(newAction(Action.Type.URL,"申請帳單分期","https://www.citibank.com.tw/sim/zh-tw/loans/index.htm"));
                column.addExternalActions(newAction(Action.Type.URL,"立刻繳款","https://www.citibank.com.tw/sim/zh-tw/loans/index.htm"));
                msgcrl.addColumn(column);
            }
            menu.addMessage(msgcrl);
            
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    @Test
    public void SendAgain003() {
    	MessageCarousel msgcrl = new MessageCarousel();
        msgcrl.setId(CitiUtil.robotID);
        msgcrl.setType(Message.Type.CAROUSEL);
        
        List<Info> infos = cardinfo.getInfos();
        for (Info info:infos) {
        	Column column = new Column();
            try {
            	column.setImageUrl("https://pic.pimg.tw/chochoc/1527921496-567262214.jpg");
                column.setImageText(info.getCardno().replaceFirst(".*(\\d{4})", "xxx$1"));
                column.setTitle("PChome Prime 聯名御璽悠遊卡" + (info.getCcl().equals("N") ? CitiUtil.sharingQuota:"" ));
                //column.setTitle( (Calendar.getInstance().get(Calendar.MONTH)+1) + CitiUtil.checkoutBill);
                //column.setTitleBackgroundColor(CitiUtil.titleBackgroundColor);
                column.addContent( newContent(Content.Type.TEXT, CitiUtil.checkoutBill + info.getStmtday() ) );
                column.addContent( newContent(Content.Type.TEXT, CitiUtil.emailBox + info.getEmail() ) );
                //column.addInternalActions(newAction(Action.Type.URL,"帳單資訊",""));
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            msgcrl.addColumn(column);
            
        }
        menu.addMessage(msgcrl);
        
        MessageButtons msgbtn = new MessageButtons();
        msgbtn.setId(CitiUtil.robotID);
        msgbtn.setType(Message.Type.BUTTONS);
        msgbtn.setText("e起愛地球救救北極熊，註冊電子月結單讓你對帳最簡單。");
        msgbtn.addAction(newAction(Action.Type.URL,"立即註冊","https://www.citibank.com.tw/sim/zh-tw/loans/index.htm"));
        menu.addMessage(msgbtn);
    }
    
    @Test
    public void CardEquity004() {
        MessageCarousel msgcrl = new MessageCarousel();
        msgcrl.setId("4eba966b-daa7-4f18-baf3-f8c1ebc9ddd3");
        msgcrl.setType(Message.Type.CAROUSEL);
        List<Info> infos = cardinfo.getInfos();
        for (Info info:infos) {   
            Column column = new Column();
            try {
                column.setImageUrl("https://www.citibank.com.tw/card.jpg");
                column.setImageText(info.getCardno().replaceFirst(".*(\\d{4})", "xxx$1"));
                column.setTitle("PChome Prime 聯名御璽悠遊卡");
                Content content = new Content();
                content.setText("6%<br>PChome消費<br>最高回饋");
                content.setType(Content.Type.TEXT);
                column.addContent(content);
                content = new Content();
                content.setText("1.5%<br>海外消費<br>回饋");
                content.setType(Content.Type.TEXT);
                column.addContent(content);
                content = new Content();
                content.setText("$0<br>海外消費<br>回饋");
                content.setType(Content.Type.TEXT);
                column.addContent(content);
                Action action = new Action();
                action.setType(Action.Type.URL);
                action.setText("了解更多");
                action.setUrl("");
                column.addInternalActions(action);
                column.addInternalActions(new Action(Action.Type.URL,"馬上申請","",null));
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            msgcrl.addColumn(column);
        }
        menu.addMessage(msgcrl);
    }
    
    @Test
    public void BillDivLoan005() {
        Info info = cardinfo.getInfos().get(0);
        BoxGrid box = new BoxGrid();
        box.setId(CitiUtil.robotID);
        box.setType(Box.Type.GRID);
        box.setTitle(CitiUtil.currBillTranDetail);
        box.addHeader(new Header("交易日",null));
        box.addHeader(new Header("請款日",null));
        box.addHeader(new Header("金額","right"));
        GridRow row = new GridRow();
        row.setTitle(info.getCardno().replaceFirst(".*(\\d{4})", "主卡$1新增消費"));
        row.setIsTitle(true);
        box.addRow(row);
        int newLineCnt = 0;
        for (Txn txn:info.getTxns()) {
            row = new GridRow();
            Field field = new Field();
            field.setText(txn.getTxndate());
            row.addField(field);
            field = new Field();
            field.setText(txn.getDate());
            row.addField(field);
            field = new Field();
            field.setText(txn.getDesc());
            row.addField(field);
            field = new Field();
            field.setText( CitiUtil.formatMoney(txn.getAmount(), CitiUtil.fontColor.BLUE) );
            row.addField(field);
            if (newLineCnt > 0)
                row.setIsAlternatingRow(true);
            newLineCnt++;
            box.addRow(row);
        }
        menu.setBox(box);
    }
    
    @Test
    public void BillRecord006() {
        MessageCarousel msgcrl = new MessageCarousel();
        msgcrl.setId(CitiUtil.robotID);
        msgcrl.setType(Message.Type.CAROUSEL);
        List<Info> infos = cardinfo.getInfos();
        for (Info info:infos) {
            Column column = new Column();
            try {
                column.setImageUrl("https://www.citibank.com.tw/card.jpg");
                column.setImageText(info.getCardno().replaceFirst(".*(\\d{4})", "xxx$1"));
                column.setTitle("PChome Prime 聯名御璽悠遊卡" + (info.getCcl().equals("N") ? CitiUtil.sharingQuota:"" ));
                Content content = new Content();
                content.setText("PChome Prime 聯名御璽悠遊卡" + (info.getCcl().equals("N") ? CitiUtil.sharingQuota:"" ));
                content.setType(Content.Type.GRID);
                content.addHeader(new Header("繳款日",null));
                content.addHeader(new Header("完成日",null));
                content.addHeader(new Header("金額","right"));
                int newLineCnt = 0;
                for (Pym pym:info.getPyms()) {
                    Row row = new Row();
                    Field field = new Field();
                    field.setText(pym.getTxndate());
                    row.addField(field);
                    field = new Field();
                    field.setText(pym.getDuedate());
                    row.addField(field);
                    field = new Field();
                    field.setText("");
                    row.addField(field);
                    if (newLineCnt > 0)
                        row.setIsAlternatingRow(true);
                    newLineCnt++;
                    content.addRow(row);
                    row = new Row();
                    field = new Field();
                    field.setText(pym.getTxndesc());
                    row.addField(field);
                    field = new Field();
                    field.setText("");
                    row.addField(field);
                    field = new Field();
                    field.setText(CitiUtil.formatMoney(pym.getTxnamount(), CitiUtil.fontColor.BLUE));
                    field.setIsBold(true);
                    field.setAlign("right");
                    row.addField(field);
                    content.addRow(row);
                }    
                column.addContent(content);
                
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            msgcrl.addColumn(column);
        }
        menu.addMessage(msgcrl);
    }
    
    
}
