package com.intumit.solr.util.chinesetranslate;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.htmlparser.jericho.Source;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.thoughtworks.xstream.XStream;

public class Config {
	public static String hansFile = "zhHans.xml";
	public static String hantFile = "zhHant.xml";

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Map<String, String> pairs = allHansMap();
		System.out.println(pairs.size());
		FileUtils.writeStringToFile(new File(Config.hansFile), new XStream()
				.toXML(pairs), "utf8");

		pairs = allHantMap();
		System.out.println(pairs.size());
		FileUtils.writeStringToFile(new File(Config.hantFile), new XStream()
				.toXML(pairs), "utf8");
	}

	public static Map<String, String> allHansMap() throws Exception {
		Map<String, String> pairs = new StringLengthOrderMap();
		pairs.putAll(phpzh2HansMap());
		pairs.putAll(phpzh2CNMap());
		pairs.putAll(zhHansMap());
		return pairs;
	}

	public static Map<String, String> allHantMap() throws Exception {
		Map<String, String> pairs = new StringLengthOrderMap();
		pairs.putAll(phpzh2HantMap());
		pairs.putAll(phpzh2TWMap());
		pairs.putAll(zhHantMap());
		return pairs;
	}

	private static Map<String, String> zhHantMap() throws Exception {
		Source source = new Source(
				new URL(
						"http://zh.wikipedia.org/zh-tw/MediaWiki:Conversiontable/zh-hant"));
		String otext = source.getTextExtractor().toString();
		return parseTable(otext, "-\\{([^}]+)\\}-", "([^ ;]+)=>([^/ ;]+)");
	}

	private static Map<String, String> phpzh2TWMap() throws Exception {
		URL url = new URL(
				"http://svn.wikimedia.org/svnroot/mediawiki/trunk/phase3/includes/ZhConversion.php");
		String otext = IOUtils.toString(url.openStream(), "utf-8");
		return parseTable(otext, "\\$zh2TW = array\\(([^)]+)\\)",
				"'([^' ]+)' => '([^' ]+)'");
	}

	private static Map<String, String> phpzh2HantMap() throws Exception {
		URL url = new URL(
				"http://svn.wikimedia.org/svnroot/mediawiki/trunk/phase3/includes/ZhConversion.php");
		String otext = IOUtils.toString(url.openStream(), "utf-8");
		return parseTable(otext, "\\$zh2Hant = array\\(([^)]+)\\)",
				"'([^' ]+)' => '([^' ]+)'");
	}

	public static Map<String, String> phpzh2HansMap() throws Exception {
		URL url = new URL(
				"http://svn.wikimedia.org/svnroot/mediawiki/trunk/phase3/includes/ZhConversion.php");
		String otext = IOUtils.toString(url.openStream(), "utf-8");
		return parseTable(otext, "\\$zh2Hans = array\\(([^)]+)\\)",
				"'([^' ]+)' => '([^' ]+)'");
	}

	public static Map<String, String> phpzh2CNMap() throws Exception {
		URL url = new URL(
				"http://svn.wikimedia.org/svnroot/mediawiki/trunk/phase3/includes/ZhConversion.php");
		String otext = IOUtils.toString(url.openStream(), "utf-8");
		return parseTable(otext, "\\$zh2CN = array\\(([^)]+)\\)",
				"'([^' ]+)' => '([^' ]+)'");
	}

	public static Map<String, String> zhHansMap() throws Exception {
		Source source = new Source(
				new URL(
						"http://zh.wikipedia.org/zh-tw/MediaWiki:Conversiontable/zh-hans"));
		String otext = source.getTextExtractor().toString();
		return parseTable(otext, "-\\{([^}]+)\\}-", "([^ ;]+)=>([^/ ;]+)");
	}

	private static Map<String, String> parseTable(String otext,
			String tablePattern, String pairPattern) {
		Pattern tableP = Pattern.compile(tablePattern);
		Pattern pairP = Pattern.compile(pairPattern);
		Map<String, String> pairs = new StringLengthOrderMap();
		Matcher m1 = tableP.matcher(otext);
		while (m1.find()) {
			Matcher m2 = pairP.matcher(m1.group(1));
			while (m2.find()) {
				pairs.put(m2.group(1), m2.group(2));
			}
		}
		return pairs;
	}

}
