package com.intumit.citi;

import java.io.*;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intumit.citi.backend.*;
import com.intumit.citi.frontend.*;
import com.intumit.solr.robot.*;
import com.intumit.solr.tenant.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.wink.json4j.*;

// 擴展 HttpServlet 類
@WebServlet(urlPatterns = { "/wiseadm/citi/detail", "/wiseadm/citi/detail/*" })
public class Detail extends HttpServlet {
 
  public void init() throws ServletException
  {
  
  }

  public void doGet(HttpServletRequest request,
          HttpServletResponse response)
          throws ServletException, IOException
  {
      response.setContentType("text/plain; charset=UTF-8");
      response.setCharacterEncoding("UTF-8");
      PrintWriter out = response.getWriter();
      //CitiDeep detail = CitiDeep.alist(request.getParameter("cardtype"));
      if(StringUtils.isNotBlank(request.getParameter("insert")))
      {   
          CitiDeep.save(1,"807","Prestige卡","9B2E3BB4.png","第4晚","全球精選飯店<br>免費住宿","8次/年","免費國際機場<br>接或送服務","無限次","使用全球<br>機場貴賓室","「若您欲申辦Citi Prestige，請來電<a href=\"tel:02-2183-8585\">(02)2183-8585</a>，將由專人為您服務，謝謝」","https://www.citibank.com.tw/global_docs/chi/cc/160121_prestige/index.htm","紅利點數",1);
		  CitiDeep.save(2,"559","寰旅尊尚世界悠遊卡","EE1C9737.png","4次/年","免費國際機場<br>接或送服務","$20/哩","不分國內外消費","無限次","使用全球<br>機場貴賓室","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=1&Media=407","https://www.citibank.com.tw/sim/zh-tw/credit-cards/premiermilesworld-card.htm","哩程",2);
		CitiDeep.save(3,"558","寰旅尊尚世界卡","EE1C9737.png","4次/年","免費國際機場<br>接或送服務","$20/哩","不分國內外消費","無限次","使用全球<br>機場貴賓室","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=1&Media=407","https://www.citibank.com.tw/sim/zh-tw/credit-cards/premiermilesworld-card.htm","哩程",2);
		CitiDeep.save(4,"593;595","寰旅世界悠遊卡","EE1C9737.png","$20/哩","不分國內外消費","終身有效","持卡期間哩程<br>終身有效","60家航空","哩程自由換<br>(星空、天合聯盟等)","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=1&Media=407","https://www.citibank.com.tw/sim/zh-tw/credit-cards/premiermiles-card.htm","哩程",3);
		CitiDeep.save(5,"597;554;550;510","寰旅世界卡","EE1C9737.png","$20/哩","不分國內外消費","終身有效","持卡期間哩程<br>終身有效","60家航空","哩程自由換<br>(星空、天合聯盟等)","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=1&Media=407","https://www.citibank.com.tw/sim/zh-tw/credit-cards/premiermiles-card.htm","哩程",3);
		CitiDeep.save(6,"451","現金回饋御璽悠遊卡","C8EE464D.png","1.5%","百貨、加油、生鮮量販超市<br>消費滿額可享最高回饋","0.5%","一般消費回饋","6折","最夯威秀<br>購票優惠","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=2&Media=322","https://www.citibank.com.tw/sim/zh-tw/credit-cards/cashback-card.htm","現金回饋",4);
		CitiDeep.save(7,"847;848;849","現金回饋御璽卡","C8EE464D.png","1.5%","百貨、加油、生鮮量販超市<br>消費滿額可享最高回饋","0.5%","一般消費回饋","6折","最夯威秀<br>購票優惠","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=2&Media=322","https://www.citibank.com.tw/sim/zh-tw/credit-cards/cashback-card.htm","現金回饋",4);
		CitiDeep.save(8,"028;432;439;496","現金回饋白金卡","C3132271.png","1.5%","百貨、加油、生鮮量販超市<br>消費滿額可享最高回饋","0.5%","一般消費回饋","6折","最夯威秀<br>購票優惠","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=2&Media=322","https://www.citibank.com.tw/sim/zh-tw/credit-cards/cashback-card.htm","現金回饋",4);
		CitiDeep.save(9,"461","現金回饋白金悠遊卡","C3132271.png","1.5%","百貨、加油、生鮮量販超市<br>消費滿額可享最高回饋","0.5%","一般消費回饋","6折","最夯威秀<br>購票優惠","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=2&Media=322","https://www.citibank.com.tw/sim/zh-tw/credit-cards/cashback-card.htm","現金回饋",4);
		CitiDeep.save(10,"420","PChome Prime聯名御璽悠遊卡","9060B945.png","6%","PChome消費<br>最高回饋","1.5%","海外消費回饋","$0","首年免年費","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=15&Media=381","https://www.citibank.com.tw/sim/zh-tw/credit-cards/pchome-card.htm","聯名點數",5);
		CitiDeep.save(11,"421","PChome Prime聯名白金悠遊卡","A81F3CDF.png","6%","PChome消費<br>最高回饋","1.5%","海外消費回饋","$0","首年免年費","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=15&Media=381","https://www.citibank.com.tw/sim/zh-tw/credit-cards/pchome-card.htm","聯名點數",5);
		CitiDeep.save(12,"947","饗樂生活御璽悠遊卡","D32E0061.png","王品買一送一","首次申辦，<br>首年生日享優惠","6折","最夯威秀<br>購票優惠","$15/點","王品集團指定餐廳 、<br>威秀購票享優惠","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=3&Media=315","https://www.citibank.com.tw/sim/zh-tw/credit-cards/privileges-card.htm","紅利點數",6);
		CitiDeep.save(13,"802","饗樂生活御璽卡","D32E0061.png","王品買一送一","首次申辦，<br>首年生日享優惠","6折","最夯威秀<br>購票優惠","$15/點","王品集團指定餐廳 、<br>威秀購票享優惠","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=3&Media=315","https://www.citibank.com.tw/sim/zh-tw/credit-cards/privileges-card.htm","紅利點數",6);
		CitiDeep.save(14,"462","饗樂生活白金悠遊卡","CCA0809B.png","王品買一送一","首次申辦，<br>首年生日享優惠","6折","最夯威秀<br>購票優惠","$15/點","王品集團指定餐廳 、<br>威秀購票享優惠","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=3&Media=315","https://www.citibank.com.tw/sim/zh-tw/credit-cards/privileges-card.htm","紅利點數",6);
		CitiDeep.save(15,"263;197","饗樂生活白金卡","CCA0809B.png","王品買一送一","首次申辦，<br>首年生日享優惠","6折","最夯威秀<br>購票優惠","$15/點","王品集團指定餐廳 、<br>威秀購票享優惠","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=3&Media=315","https://www.citibank.com.tw/sim/zh-tw/credit-cards/privileges-card.htm","紅利點數",6);
		CitiDeep.save(16,"948","超級紅利回饋御璽悠遊卡","3D4CFB25.png","$30/點","一般消費回饋<br>無上限","3倍","4大族群天天3倍回饋<br>(含原1倍回饋)","6折","威秀最狂<br>購票優惠","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=2&Media=313","https://www.citibank.com.tw/sim/zh-tw/credit-cards/super-rewards-card.htm","紅利點數",7);
		CitiDeep.save(17,"494","超級紅利回饋御璽卡","3D4CFB25.png","$30/點","一般消費回饋<br>無上限","3倍","4大族群天天3倍回饋<br>(含原1倍回饋)","6折","威秀最狂<br>購票優惠","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=2&Media=313","https://www.citibank.com.tw/sim/zh-tw/credit-cards/super-rewards-card.htm","紅利點數",7);
		CitiDeep.save(18,"463","超級紅利回饋白金悠遊卡","C82E091D.png","$30/點","一般消費回饋<br>無上限","3倍","4大族群天天3倍回饋<br>(含原1倍回饋)","6折","威秀最狂<br>購票優惠","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=2&Media=313","https://www.citibank.com.tw/sim/zh-tw/credit-cards/super-rewards-card.htm","紅利點數",7);
		CitiDeep.save(19,"492;018;023;130;138;230;238;333;338;417;427","超級紅利回饋白金卡","F7507041.png","$30/點","一般消費回饋<br>無上限","3倍","4大族群天天3倍回饋<br>(含原1倍回饋)","6折","威秀最狂<br>購票優惠","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=2&Media=313","https://www.citibank.com.tw/sim/zh-tw/credit-cards/super-rewards-card.htm","紅利點數",7);
		CitiDeep.save(20,"594","超級紅利回饋鈦金卡","4B1BD633.png","$30/點","一般消費回饋<br>無上限","3倍","4大族群天天3倍回饋<br>(含原1倍回饋)","6折","威秀最狂<br>購票優惠","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=2&Media=313","https://www.citibank.com.tw/sim/zh-tw/credit-cards/super-rewards-card.htm","紅利點數",7);
		CitiDeep.save(21,"241;266","HAPPY GO聯名卡","78C2252F.png","1倍","一般消費單筆每滿$100<br/>享HAPPY GO點數1點","2倍","特約商店點數<br/>2倍回饋","3倍","5大百貨/商店<br/>點數3倍回饋","請來電<a href=\"tel:02-2183-8585\">(02)2183-8585</a>，將由專人為您服務，謝謝","https://www.citibank.com.tw/global_docs/chi/cc/120604_happygo/index.htm","HAPPY GO點數",8);
		CitiDeep.save(22,"256;808","現金回饋PLUS御璽悠遊卡","PLUS_CARDFACE.png","1","等待上線","2","等待上線","3","等待上線","不提供新卡申請","https://www.citibank.com.tw/sim/index.htm","現金回饋",9);
		CitiDeep.save(23,"400;499","鑽石卡","D8E8D615.png","2次/年","免費國際機場<br>接或送服務","5★級","飯店頂級饗宴<br/>最優75折","無限次","使用全球<br>機場貴賓室","不提供新卡申請","https://www.citibank.com.tw/global_docs/chi/cc/diamond2013/page1_2.htm","紅利點數",10);
		CitiDeep.save(24,"599;330;500","鑽石卡","965645EB.png","2次/年","免費國際機場<br>接或送服務","5★級","飯店頂級饗宴<br/>最優75折","無限次","使用全球<br>機場貴賓室","不提供新卡申請","https://www.citibank.com.tw/global_docs/chi/cc/diamond2013/page1_2.htm","紅利點數",10);
		CitiDeep.save(25,"454;450;410;567;151;251;200","寰旅御璽卡","A8960231.png","終身有效","持卡期間哩程<br>終身有效","$20/哩","不分國內外消費","60家航空","哩程自由換<br>(星空、天合聯盟等)","不提供新卡申請","https://www.citibank.com.tw/global_docs/chi/ap/ap_premiermiles-signature_manual.pdf","哩程",11);
		CitiDeep.save(26,"845;804","現金回饋無限悠遊卡","8C93276F.png","0.88%","國內一般<br>消費回饋","1.5%","海外消費回饋","4次/年","桃園/高雄國際機場<br>外圍免費停車","不提供新卡申請","https://www.citibank.com.tw/sim/zh-tw/credit-cards/cashback-card-infinite.htm","現金回饋",12);
		CitiDeep.save(27,"803;846","現金回饋無限卡","8C93276F.png","0.88%","國內一般<br>消費回饋","1.5%","海外消費回饋","4次/年","桃園/高雄國際機場<br>外圍免費停車","不提供新卡申請","https://www.citibank.com.tw/sim/zh-tw/credit-cards/cashback-card-infinite.htm","現金回饋",12);
		CitiDeep.save(28,"440;484","透明卡","15A27467.png","2%","國內加油費、行動通話費<br>滿額享最高回饋","$30/點","一般消費回饋<br>無上限","6折","威秀最狂<br>購票優惠","不提供新卡申請","https://www.citibank.com.tw/sim/zh-tw/credit-cards/clear-card.htm","紅利點數/現金回饋",13);
		CitiDeep.save(29,"584;540","透明卡","61B5420D.png","2%","國內加油費、行動通話費<br>滿額享最高回饋","$30/點","一般消費回饋<br>無上限","6折","威秀最狂<br>購票優惠","不提供新卡申請","https://www.citibank.com.tw/sim/zh-tw/credit-cards/clear-card.htm","紅利點數/現金回饋",13);
		CitiDeep.save(30,"160;260","喜憨兒卡","98C4233E.png","0.275%","捐給「喜憨兒社會福利基金會」<br>(提撥自每筆消費)","終身有效","持卡期間點數<br>終身有效","2000萬","公共運輸<br>旅行平安險","不提供新卡申請","https://www.citibank.com.tw/sim/zh-tw/credit-cards/cau-card.htm","紅利點數",14);
		CitiDeep.save(31,"650;651;652;660;680;681","大來卡","9DCEF1FC.png","","","","","","","不提供新卡申請","","",15);
		CitiDeep.save(32,"458;140;106;009;300;805;973;190","UAT專用","UAT_CARDFACE.jpg","1","PAT無此LOGO","2","PAT無此LOGO","3","PAT無此LOGO","不提供新卡申請","https://www.citibank.com.tw/sim/index.htm","",16);
		CitiDeep.save(33,"010;532;539;596;120","現金回饋白金卡","92817AE3.png","1.5%","百貨、加油、生鮮量販超市<br>消費滿額可享最高回饋","0.5%","一般消費回饋","6折","最夯威秀<br>購票優惠","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=2&Media=322","https://www.citibank.com.tw/sim/zh-tw/credit-cards/cashback-card.htm","現金回饋",4);
		CitiDeep.save(34,"004;005;334;335;517;527;592","超級紅利回饋白金卡","4B1BD633.png","$30/點","一般消費回饋<br>無上限","3倍","4大族群天天3倍回饋<br>(含原1倍回饋)","6折","威秀最狂<br>購票優惠","https://www.citibank.com.tw/credit-cards/application/index.html?CardType=2&Media=313","https://www.citibank.com.tw/sim/zh-tw/credit-cards/super-rewards-card.htm","紅利點數",7);
        Apikey apikey = Apikey.get(1L);
        apikey.setApikey("3558457160eac62a1cdd");
        Apikey.saveOrUpdate(apikey);
      }
      else
      {
          for(int i=1; i<200; i++)
          {
              CitiDeep detail = CitiDeep.get(i);
              if( detail == null )
                  continue;
              else
              /*System.*/out.println("CitiDeep.save(" + detail.getId() + ",\"" + detail.getCardType() + "\",\"" + detail.getTitle() + "\",\"" + detail.getImageUrl() + "\",\"" + detail.getOfferName1() + "\",\"" 
                                 + detail.getOfferText1() + "\",\"" + detail.getOfferName2() + "\",\"" + detail.getOfferText2() + "\",\"" + detail.getOfferName3() + "\",\"" + 
                                 detail.getOfferText3() + "\",\"" + detail.getApplyNow() + "\",\"" + detail.getKnowMore() + "\",\"" + detail.getReward() + "\"," + detail.getPriority() + ");");
          }   
      }     
  }
  
  public void destroy()
  {
      // 什麼也不做
  }
}