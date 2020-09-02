package com.intumit.android.search.fuzzy;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.intumit.android.search.FuzzyLevel;
import com.intumit.solr.util.WiSeEnv;

public class TestFuzzyMixedStringSearcher {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSearchMixChineseAndEnglishAndNumber() {
		FuzzyMixedStringSearcher fuzzy = new FuzzyMixedStringSearcher(new File(WiSeEnv.getHomePath() + "/dict/fuzzydata/fssTemp"));
		Set<String> products = new HashSet<>(
				Arrays.asList(new String[] {
						"西踢叉", 
						"Geforce西踢叉1080踢唉", 
						"Geforce CTX 1080", 
						})
				);
		
		fuzzy.createDictionary(products, "test");
		fuzzy.setFuzzyLevel(FuzzyLevel.LOW);

		Assert.assertArrayEquals(new String[] {"Geforce西踢叉1080踢唉"}, fuzzy.search("請問Geforce 西叉 1080 踢唉的driver在哪下載?", 1).getSuggests().toArray());
		Assert.assertArrayEquals(new String[] {"Geforce CTX 1080"}, fuzzy.search("請問Gforce CTX 1080 driver在哪下載?", 1).getSuggests().toArray());
	}

	@Test
	public void testSearchResultRanking() {
		FuzzyMixedStringSearcher fuzzy = new FuzzyMixedStringSearcher(new File(WiSeEnv.getHomePath() + "/dict/fuzzydata/fssTemp"));
		Set<String> products = new HashSet<>(
				Arrays.asList(new String[] {
						"r281t91 rev 100",
						"r281z91 rev 100",
						"r2813c1 rev 100",
						"r281z92 rev 100",
						"r181z91 rev 100",
						"r181t90 rev 100",
						})
				);
		
		fuzzy.createDictionary(products, "test");
		fuzzy.setFuzzyLevel(FuzzyLevel.LOW);
		
		Assert.assertSame("r281t91 rev 100", fuzzy.search("r281t91 rev 100查詢").getSuggests().get(0));
		Assert.assertSame("r281t91 rev 100", fuzzy.search("r282t91 rev 100查詢").getSuggests().get(0));
		Assert.assertSame("r281z91 rev 100", fuzzy.search("r281c91 rev 100查詢").getSuggests().get(0));
		Assert.assertSame("r281z91 rev 100", fuzzy.search("r28191 rev 100查詢").getSuggests().get(0));
	}

	@Test
	public void testSearchOnLongDictionaryEntry() {
		FuzzyMixedStringSearcher fuzzy = new FuzzyMixedStringSearcher(new File(WiSeEnv.getHomePath() + "/dict/fuzzydata/fssTemp"));
		Set<String> products = new HashSet<>(
				Arrays.asList(new String[] {
						"geforce gtx 1070i xtreme gaming 8g rev10", 
						"qrxtuvwxyzabc abcdefg hij lmnopc", 
						"abcdefg hij lmnopc qrxtuvwxyza", 
						})
				);
		
		fuzzy.createDictionary(products, "test");
		fuzzy.setFuzzyLevel(FuzzyLevel.LOW);

		Assert.assertArrayEquals(new String[] {"abcdefg hij lmnopc qrxtuvwxyza"}, fuzzy.search("abcdefg hij lmnopc qxtuvwxyza xyzab cd efghi", 1).getSuggests().toArray());
		Assert.assertArrayEquals(new String[] {"abcdefg hij lmnopc qrxtuvwxyza"}, fuzzy.search("abcdefg hij lmnopc qrxtuvwxyza xyzab cd efghi", 1).getSuggests().toArray());
		Assert.assertArrayEquals(new String[] {"qrxtuvwxyzabc abcdefg hij lmnopc"}, fuzzy.search("qrxtuvwxyza abcdefg hij lmnopc xyzab cd efghi", 1).getSuggests().toArray());
		Assert.assertArrayEquals(new String[] {"qrxtuvwxyzabc abcdefg hij lmnopc"}, fuzzy.search("qrxtuvwxyzabc abcdefg hij lmnopc", 1).getSuggests().toArray());
		Assert.assertArrayEquals(new String[] {"qrxtuvwxyzabc abcdefg hij lmnopc"}, fuzzy.search("qrxtuvwxyzabc abcdefg hij lmnopc xyzab cd efghi", 1).getSuggests().toArray());
		Assert.assertArrayEquals(new String[] {"geforce gtx 1070i xtreme gaming 8g rev10"}, fuzzy.search("geforce gtx 1070i xtreme gaming 8g rev10", 1).getSuggests().toArray());
	}

	@Test
	public void testChineseFuzzy() {
		FuzzyMixedStringSearcher fuzzy = new FuzzyMixedStringSearcher(new File(WiSeEnv.getHomePath() + "/dict/fuzzydata/fssTemp"));
		List<String> allDict = new ArrayList<String>();
		try {
			allDict.addAll(FileUtils.readLines(new File("test/test-files/fuzzy/productLine.dic"), "UTF-8"));
		}
		catch (IOException e) {
			System.out.println("PrepareForCustomQARule import dic err: " + e);
		}
		Set<String> allDictSet = new HashSet<>(Arrays.asList(allDict.toArray(new String[allDict.size()])));
		fuzzy.createDictionary(allDictSet, "test");
		fuzzy.setFuzzyLevel(FuzzyLevel.LOW);

		System.out.println(fuzzy.search("我要查詢筆記型電腦").getSuggests());
		Assert.assertArrayEquals(new String[] {"筆記型電腦", "超微型電腦"}, fuzzy.search("我要查詢筆記型電腦").getSuggests().toArray());
		Assert.assertArrayEquals(new String[] {"筆記型電腦", "超微型電腦"}, fuzzy.search("我要查詢筆型電腦").getSuggests().toArray());

	}

	@Test
	public void testSSD() {
		FuzzyMixedStringSearcher fuzzy = new FuzzyMixedStringSearcher(new File(WiSeEnv.getHomePath() + "/dict/fuzzydata/fssTemp"));
		Set<String> products = new HashSet<>(
				Arrays.asList(new String[] {
						"固態硬碟 ssd", 
						})
				);
		
		fuzzy.createDictionary(products, "test");
		fuzzy.setFuzzyLevel(FuzzyLevel.LOW);
		
		System.out.println(fuzzy.search("固態硬碟 ssd").getSuggests());
		Assert.assertArrayEquals(new String[] {"固態硬碟 ssd"}, fuzzy.search("固態硬碟ssd").getSuggests().toArray());
		Assert.assertArrayEquals(new String[] {"固態硬碟 ssd"}, fuzzy.search("固態硬碟 ssd").getSuggests().toArray());
		Assert.assertArrayEquals(new String[] {"固態硬碟 ssd"}, fuzzy.search("固態硬碟ssd查詢").getSuggests().toArray());
	}
}
