package com.intumit.solr.highlighter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.solr.highlight.SolrHighlighter;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.util.AbstractSolrTestCase;
import org.apache.solr.util.TestHarness;

/**
 * Tests some basic functionality of Solr while demonstrating good
 * Best Practices for using AbstractSolrTestCase
 */
public class HighlighterTest extends AbstractSolrTestCase {

  private static String LONG_TEXT = "a long days night this should be a piece of text which is is is is is is is is is is is is is is is is is is is " +
          "is is is is is isis is is is is is is is is is is is is is is is is is is is is is is is is is is is is is is is is is is is is is is is is is " +
          "is is is is is is is is is is is is is " +
          "is is is is is is is is is is is is is is is is is is is is sufficiently lengthly to produce multiple fragments which are not concatenated " +
          "at all--we want two disjoint long fragments.";

//  @Override public String getSchemaFile() { return "schema.xml"; }
//  @Override public String getSolrConfigFile() { return "solrconfig-multivalued-highlighter.xml"; }

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
  

  public void testTermVecMultiValuedHighlight() throws Exception {

    // do summarization using term vectors on multivalued field
    HashMap<String,String> args = new HashMap<String,String>();
    args.put("hl", "true");
    args.put("hl.fl", "tv_mv_text");
    args.put("hl.snippets", "2");
    TestHarness.LocalRequestFactory sumLRF = h.getRequestFactory(
      "standard",0,200,args);
    
    assertU(adoc("tv_mv_text", LONG_TEXT, 
                 "tv_mv_text", LONG_TEXT, 
                 "id", "1"));
    assertU(commit());
    assertU(optimize());
    assertQ("Basic summarization",
            sumLRF.makeRequest("tv_mv_text:long"),
            "//lst[@name='highlighting']/lst[@name='1']",
            "//lst[@name='1']/arr[@name='tv_mv_text']/str[.='a <em>long</em> days night this should be a piece of text which']",
            "//arr[@name='tv_mv_text']/str[.=' <em>long</em> fragments.']"
            );
  }
  
  
  public void testMultiValueBestFragmentHighlight() throws IOException, Exception {
    HashMap<String,String> args = new HashMap<String,String>();
    args.put("hl", "true");
    args.put("hl.fl", "textgap");
    args.put("df", "textgap");
    TestHarness.LocalRequestFactory sumLRF = h.getRequestFactory(
        "standard", 0, 200, args);
    
    assertU(adoc("textgap", "first entry has one word foo", 
        "textgap", "second entry has both words foo bar",
        "id", "1"));
    assertU(commit());
    assertU(optimize());
    System.out.println(h.query(sumLRF.makeRequest("foo bar")));
    assertQ("Best fragment summarization",
        sumLRF.makeRequest("foo bar"),
        "//lst[@name='highlighting']/lst[@name='1']",
        "//lst[@name='1']/arr[@name='textgap']/str[.=\'second entry has both words <em>foo</em> <em>bar</em>\']"
    );
  }
}
