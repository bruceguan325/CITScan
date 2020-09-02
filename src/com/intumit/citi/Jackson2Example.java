package com.intumit.citi;

import java.io.IOException;
import java.net.*;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.*;
import com.intumit.citi.frontend.*;

/**開始與迪普文字客服串接撰寫範例測試用
 * @author user
 *
 */
public class Jackson2Example {

    public static void main(String[] args) {
        Jackson2Example obj = new Jackson2Example();
        obj.run(Integer.parseInt(args[0]));
    }

    private void run(int type) {
        ObjectMapper mapper = new ObjectMapper();

        Menu staff = createDummyObject(type);

        try {
            // Convert object to JSON string and save into a file directly
            //mapper.writeValue(new File("D:\\staff.json"), staff);

            // Convert object to JSON string
            String jsonInString = mapper.writeValueAsString(staff);
            System.out.println(jsonInString);

            // Convert object to JSON string and pretty print
            jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(staff);
            System.out.println(jsonInString);

        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Menu createDummyObject(int type) {

        Menu menu = new Menu();
        Result result = new Result(0,"");
        menu.setResult(result);
        Action action;
        Column column;
        MessageCarousel msgcrl;
        Content content;
        
        switch(type)
        {
        case 1:
        	MessageText msgtxt = new MessageText();
            msgtxt.setId(CitiUtil.robotID);
            msgtxt.setType(Message.Type.TEXT);
            msgtxt.setText("簡單3步驟立即設定推撥通知，讓您越刷越安心 ~ ..");
            menu.addMessage(msgtxt);
        	break;
        case 2:
        	MessageButtons msgbtn = new MessageButtons();
            msgbtn.setId(CitiUtil.robotID);
            msgbtn.setType(Message.Type.BUTTONS);
            msgbtn.setText("好的，請您先選擇。");
            action = new Action();
            action.setText("帳單分期");
            action.setType(Action.Type.URL);
            try {
                action.setUrl("https://www.citibank.com.tw/global_docs/chi/cc/161013_rte/index2_pc.html");
                msgbtn.addAction(action);
                msgbtn.addAction(new Action(Action.Type.URL,"我要辦信貸","https://www.citibank.com.tw/sim/zh-tw/loans/index.htm",null));
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            menu.addMessage(msgbtn);
        	break;
        case 3:
            msgcrl = new MessageCarousel();
            msgcrl.setId("4eba966b-daa7-4f18-baf3-f8c1ebc9ddd3");
            msgcrl.setType(Message.Type.CAROUSEL);
            column = new Column();
            try {
                column.setImageUrl("https://www.citibank.com.tw/card.jpg");
                column.setImageText("xxx8899");
                column.setImageDesc("PChome Prime 聯名御璽悠遊卡");
                column.setTitle("7/4結帳帳單");
                column.setTitleBackgroundColor("#7F7F7F");
                content = new Content();
                content.setText("預計一小時內寄到您的電子信箱…");
                content.setType(Content.Type.TEXT);
                column.addContent(content);
                action = new Action();
                action.setType(Action.Type.URL);
                action.setText("帳單資訊");
                action.setUrl("");
                column.addInternalActions(action);
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            msgcrl.addColumn(column);
            menu.addMessage(msgcrl);
        	break;
        case 4:
        	break;
        case 5:
        	msgcrl = new MessageCarousel();
            msgcrl.setId("4eba966b-daa7-4f18-baf3-f8c1ebc9ddd3");
            msgcrl.setType(Message.Type.CAROUSEL);
            column = new Column();
            try {
                column.setImageUrl("https://www.citibank.com.tw/card.jpg");
                column.setImageText("xxx8899");
                column.setTitle(" PChome Prime 聯名御璽悠遊卡");
                content = new Content();
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
                action = new Action();
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
            menu.addMessage(msgcrl);
        	break;
        case 6:
        	msgcrl = new MessageCarousel();
            msgcrl.setId("4eba966b-daa7-4f18-baf3-f8c1ebc9ddd3");
            msgcrl.setType(Message.Type.CAROUSEL);
            column = new Column();
            try {
            	column.setImageUrl("https://www.citibank.com.tw/card.jpg");
                column.setImageText("xxx8899");
                column.setTitle(" PChome Prime 聯名御璽悠遊卡(共用額度)");
                content = new Content();
                content.setText(" PChome Prime 聯名御璽悠遊卡(共用額度)");
                content.setType(Content.Type.GRID);
                content.addHeader(new Header("繳款日",null));
                content.addHeader(new Header("完成日",null));
                content.addHeader(new Header("金額","right"));
                Row row = new Row();
                Field field = new Field();
                field.setText("9/20");
                row.addField(field);
                field = new Field();
                field.setText("9/27");
                row.addField(field);
                field = new Field();
                field.setText("");
                row.addField(field);
                content.addRow(row);
                row = new Row();
                field = new Field();
                field.setText("親臨本行繳款已入帳");
                row.addField(field);
                field = new Field();
                field.setText("");
                row.addField(field);
                field = new Field();
                field.setText("$2,400");
                field.setIsBold(true);
                field.setAlign("right");
                row.addField(field);
                content.addRow(row);
                row = new Row();
                row.setIsAlternatingRow(true);
                field = new Field();
                field.setText("9/15");
                row.addField(field);
                field = new Field();
                field.setText("9/22");
                row.addField(field);
                field = new Field();
                field.setText("");
                row.addField(field);
                content.addRow(row);
                row = new Row();
                field = new Field();
                field.setText("提款機繳款已入帳");
                row.addField(field);
                field = new Field();
                field.setText("");
                row.addField(field);
                field = new Field();
                field.setText("$1,000");
                field.setIsBold(true);
                field.setAlign("right");
                row.addField(field);
                content.addRow(row);
                
                column.addContent(content);
                
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            msgcrl.addColumn(column);
            menu.addMessage(msgcrl);
        	
        	break;
        case 7:
            BoxGrid boxgrid = new BoxGrid();
        	boxgrid.setId("4eba966b-daa7-4f18-baf3-f8c1ebc9ddd3");
            boxgrid.setType(Box.Type.GRID);
            boxgrid.setTitle("本期帳單交易明細");
            {
             	boxgrid.addHeader(new Header("交易日",null));
                boxgrid.addHeader(new Header("請款日",null));
                boxgrid.addHeader(new Header("金額","right"));
                GridRow row = new GridRow();
                row.setTitle("主卡8899新增消費");
                row.setIsTitle(true);
                boxgrid.addRow(row);
                row = new GridRow();
                Field field = new Field();
                field.setText("9/20");
                row.addField(field);
                field = new Field();
                field.setText("9/27");
                row.addField(field);
                field = new Field();
                field.setText("");
                row.addField(field);
                boxgrid.addRow(row);
                row = new GridRow();
                field = new Field();
                field.setText("eTag自動儲值");
                row.addField(field);
                field = new Field();
                field.setText("");
                row.addField(field);
                field = new Field();
                field.setText("$720");
                field.setIsBold(true);
                field.setAlign("right");
                row.addField(field);
                boxgrid.addRow(row);
                row = new GridRow();
                row.setIsAlternatingRow(true);
                field = new Field();
                field.setText("9/15");
                row.addField(field);
                field = new Field();
                field.setText("9/22");
                row.addField(field);
                field = new Field();
                field.setText("");
                row.addField(field);
                boxgrid.addRow(row);
                   
            }
            menu.setBox(boxgrid);
        	
            break;
        case 8:
            BoxAnnounce boxAnn = new BoxAnnounce();
        	boxAnn.setId("4eba966b-daa7-4f18-baf3-f8c1ebc9ddd3");
            boxAnn.setType(Box.Type.ANNOUNCE);
            boxAnn.setTitle("緊急公告");
            boxAnn.addContent(new BoxCon("颱風通知", "南區銀行於 2018/06/18 ，魔羯颱風來襲暫停營業，造成您的不便，請見諒！"));
            boxAnn.addContent(new BoxCon("停電通知", "板橋分行於 2018/06/08 ，施工造成停電暫停服務，造成您的不便，請見諒！"));
            menu.setBox(boxAnn);
        	
            break;
        }
        
        return menu;

    }

}