package com.intumit.solr;


import org.apache.solr.util.AbstractSolrTestCase;

/**
 * Tests some basic functionality of Solr while demonstrating good
 * Best Practices for using AbstractSolrTestCase
 */
public class SearchTest extends AbstractSolrTestCase {

  private static String DOUBLE_LONG_TEXT = "這是一篇測試用的文章，包含全形的標點符號、數字以及各種括弧等等" +
          "例如我們可以測試（千里迢迢）以及「碩網資訊」和『大中至正』。" +
          "又可以測試１２３４＜９８３４７５＞！！＠＃＄％︿" +
          "還要 ABC:DEF 測試日文プロ野球福岡ヤフードームロンドン 等漢字或片假名\n" +
          "向左走-向右走\n" +
          "以及中英混雜的N-900時候要怎麼判斷ＩＮＴＵＭＩＴ碩網資訊";

  private static String DOUBLE_LONG_TEXT2 = "標點符號數字1234(983475)必勝INTUMIT必勝";
  private static String DOUBLE_LONG_TEXT3 = "信長の野望";
  
//  @Override public String getSchemaFile() { return "solr/conf/schema-cjk.xml"; }
//  @Override public String getSolrConfigFile() { return "solr/conf/solrconfig.xml"; }

  @Override 
  public void setUp() throws Exception {
    // if you override setUp or tearDown, you better call
    // the super classes version
    super.setUp();
  }
  
  @Override 
  public void tearDown() throws Exception {
    // if you override setUp or tearDown, you better call
    // the super classes version
    super.tearDown();
  }
  
  public void testDoubleBytes() {
	  String f = "Name_t";
	  assertU(adoc("id", "1", f, DOUBLE_LONG_TEXT));
	  assertU(adoc("id", "2", f, DOUBLE_LONG_TEXT2));
	  assertU(adoc("id", "3", f, DOUBLE_LONG_TEXT3));
	  assertU(commit());
	  assertQ("測試中文",
	            req(f + ":碩網")
	            ,"//result[@numFound='1']"
	            );  
	  assertQ("測試中文2",
	            req(f + ":數字")
	            ,"//result[@numFound='2']"
	            );  
	  assertQ("測試日文",
	            req(f + ":プロ野球")
	            ,"//result[@numFound='1']"
	            );  
	  assertQ("測試日文2",
	            req(f + ":ヤフードームロンドン")
	            ,"//result[@numFound='1']"
	            );  
	  assertQ("測試日文3",
	            req(f + ":ムロ")
	            ,"//result[@numFound='1']"
	            );  
	  /*assertQ("測試日文的「の」",
	            req(f + ":信長の野望")
	            ,"//result[@numFound='1']"
	            );  
	  assertQ("測試日文的「の」2",
	            req(f + ":信長野望")
	            ,"//result[@numFound='0']"
	            );  
	  assertQ("測試日文的「の」3",
	            req(f + ":信長,野望")
	            ,"//result[@numFound='1']"
	            );  */
	  assertQ("測試全形數字",
	            req(f + ":１２３４")
	            ,"//result[@numFound='2']"
	            );  
	  assertQ("測試全形數字2",
	            req(f + ":９８３４７５")
	            ,"//result[@numFound='2']"
	            );  
	  assertQ("測試全形數字3",
	            req(f + ":＜９８３４７５＞")
	            ,"//result[@numFound='2']"
	            );  
	  assertQ("測試全形數字4",
	            req(f + ":９８３４")
	            ,"//result[@numFound='0']"
	            );  
	  assertQ("測試只查全形標點符號",
	            req(f + ":！！")
	            ,"//result[@numFound='0']"
	            );  
	  assertQ("測試全形標點符號加上文字",
	            req(f + ":標點符號、數字")
	            ,"//result[@numFound='1']"
	            );
	  assertQ("測試半形標點符號加上文字（但實際文章是全形標點符號）",
	            req(f + ":標點符號,數字")
	            ,"//result[@numFound='1']"
	            );
	  assertQ("測試半形標點符號加上空白才接文字（但實際文章是全形標點符號），空白會被視為 OR",
	            req(f + ":標點符號, 數字")
	            ,"//result[@numFound='2']"
	            );
  	  assertQ("測試全形括弧",
            req(f + ":「碩網資訊」")
            ,"//result[@numFound='1']"
            );
  	  assertQ("測試全形括弧2",
              req(f + ":『碩網資訊』")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試半形英文（資料是全形英文）",
              req(f + ":intumit")
              ,"//result[@numFound='2']"
              );
  	  assertQ("測試英文1",
              req(f + ":DEF")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試英文2",
              req(f + ":ABC")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試英文3",
              req(f + ":ABC-DEF")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試英文4",
              req(f + ":ABCDEF")
              ,"//result[@numFound='0']"
              );
  	  assertQ("測試英文加上數字",
              req(f + ":N900")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試英文加上數字2",
              req(f + ":900")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試英文加上數字3",
              req(f + ":N-900")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試星號",
              req(f + ":intu*")
              ,"//result[@numFound='2']"
              );
  	  assertQ("測試問號",
              req(f + ":int?mit")
              ,"//result[@numFound='2']"
              );
  	  assertQ("測試問號2",
              req(f + ":int??it")
              ,"//result[@numFound='2']"
              );
  	  assertQ("測試問號3",
              req(f + ":int?it")
              ,"//result[@numFound='0']"
              );
  	  assertQ("測試蛇行符號",
              req(f + ":intmuit~")
              ,"//result[@numFound='2']"
              );
  	  assertQ("測試蛇行符號2",
              req(f + ":inmtuit~")
              ,"//result[@numFound='2']"
              );
  	  assertQ("測試蛇行符號3",
              req(f + ":mnituit~")
              ,"//result[@numFound='2']"
              );
  	  assertQ("測試蛇行符號4",
              req(f + ":inmmuit~")
              ,"//result[@numFound='2']"
              );
  	  assertQ("測試蛇行符號5",
              req(f + ":inbauit~")
              ,"//result[@numFound='2']"
              );
  	  assertQ("測試蛇行符號6",
              req(f + ":intum~")
              ,"//result[@numFound='2']"
              );
  	  assertQ("測試蛇行符號（中文）1",
              req(f + ":\"碩網 大中\"~4")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試蛇行符號（中文）2",
              req(f + ":\"碩 資 和\"~2")
              ,"//result[@numFound='0']"
              );
  	  assertQ("測試蛇行符號（中文）3",
              req(f + ":\"碩資和\"~2")
              ,"//result[@numFound='0']"
              );
  	  assertQ("測試標點不同",
              req(f + ":向左走,向右走")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試標點省略",
              req(f + ":向左走向右走")
              ,"//result[@numFound='0']"
              );
  }
  

  public void testDoubleBytes_For_Text_U() {
	  String f = "Name_u";
	  assertU(adoc("id", "1", f, DOUBLE_LONG_TEXT));
	  assertU(adoc("id", "2", f, DOUBLE_LONG_TEXT2));
	  assertU(adoc("id", "3", f, DOUBLE_LONG_TEXT3));
	  assertU(commit());
	  assertQ("測試中文",
	            req(f + ":碩網")
	            ,"//result[@numFound='1']"
	            );  
	  assertQ("測試中文2",
	            req(f + ":數字")
	            ,"//result[@numFound='2']"
	            );  
	  assertQ("測試日文",
	            req(f + ":プロ野球")
	            ,"//result[@numFound='1']"
	            );  
	  assertQ("測試日文2",
	            req(f + ":ヤフードームロンドン")
	            ,"//result[@numFound='1']"
	            );  
	  assertQ("測試日文3",
	            req(f + ":ムロ")
	            ,"//result[@numFound='1']"
	            );  
	  assertQ("測試全形數字",
	            req(f + ":１２３４")
	            ,"//result[@numFound='1']"
	            );  
	  assertQ("測試全形數字2",
	            req(f + ":９８３４７５")
	            ,"//result[@numFound='1']"
	            );  
	  assertQ("測試全形數字3",
	            req(f + ":＜９８３４７５＞")
	            ,"//result[@numFound='1']"
	            );  
	  assertQ("測試全形數字4",
	            req(f + ":９８３４")
	            ,"//result[@numFound='0']"
	            );  
	  assertQ("測試只查全形標點符號",
	            req(f + ":！！")
	            ,"//result[@numFound='0']"
	            );  
	  assertQ("測試全形標點符號加上文字",
	            req(f + ":標點符號、數字")
	            ,"//result[@numFound='2']"
	            );
	  assertQ("測試半形標點符號加上文字（但實際文章是全形標點符號）",
	            req(f + ":標點符號,數字")
	            ,"//result[@numFound='2']"
	            );
	  assertQ("測試半形標點符號加上空白才接文字（但實際文章是全形標點符號），空白會被視為 OR",
	            req(f + ":標點符號, 數字")
	            ,"//result[@numFound='2']"
	            );
  	  assertQ("測試全形括弧",
            req(f + ":「碩網資訊」")
            ,"//result[@numFound='1']"
            );
  	  assertQ("測試全形括弧2",
              req(f + ":『碩網資訊』")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試半形英文（資料是全形英文）",
              req(f + ":intumit")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試英文1",
              req(f + ":DEF")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試英文2",
              req(f + ":ABC")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試英文3",
              req(f + ":ABC-DEF")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試英文4",
              req(f + ":AbcDef")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試英文5",
              req(f + ":ABCDEF")
              ,"//result[@numFound='0']"
              );
  	  assertQ("測試英文加上數字",
              req(f + ":N900")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試英文加上數字2",
              req(f + ":900")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試英文加上數字3",
              req(f + ":N-900")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試星號",
              req(f + ":intu*")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試問號",
              req(f + ":int?mit")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試問號2",
              req(f + ":int??it")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試問號3",
              req(f + ":int?it")
              ,"//result[@numFound='0']"
              );
  	  assertQ("測試蛇行符號",
              req(f + ":intmuit~")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試蛇行符號2",
              req(f + ":inmtuit~")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試蛇行符號3",
              req(f + ":mnituit~")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試蛇行符號4",
              req(f + ":inmmuit~")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試蛇行符號5",
              req(f + ":inbauit~")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試蛇行符號6",
              req(f + ":intum~")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試蛇行符號（中文）1",
              req(f + ":\"碩網 大中\"~4")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試蛇行符號（中文）2",
              req(f + ":\"碩 資 和\"~2")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試蛇行符號（中文）3",
              req(f + ":\"碩資和\"~2")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試標點不同",
              req(f + ":向左走,向右走")
              ,"//result[@numFound='1']"
              );
  	  assertQ("測試標點省略",
              req(f + ":向左走向右走")
              ,"//result[@numFound='1']"
              );
  }

}
