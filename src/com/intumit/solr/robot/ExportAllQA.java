package com.intumit.solr.robot;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.dictionary.KnowledgePointDictionary;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeEnv;

public class ExportAllQA {

	public static void doIt(Tenant tenant, String type, Long runTime) {
		for (int i = 0; i < 1750; i = i + 10) {
			exportQA(tenant, type, runTime, i);
		}
	}

	public static SolrQuery getQuery(int start) {
		SolrQuery sqry = new SolrQuery();
		sqry.setQuery("*:*");
		sqry.setParam("fl", "id,QUESTION_ALT_TPL_ms");
		sqry.addFilterQuery("-isPart_i:[2 TO *]");
		sqry.addFilterQuery("dataType_s:COMMON_SENSE");
		sqry.addSort("kid_l", SolrQuery.ORDER.asc);
		sqry.setStart(start);
		sqry.setRows(10);
		return sqry;
	}

	public static SolrQuery getAltQuery(String alt) {
		SolrQuery sqry = new SolrQuery();
		sqry.setQuery("*:*");
		sqry.setParam("fl", QAUtil.QA_RESPONSE_FIELDS);
		sqry.addFilterQuery("dataType_s:COMMON_SENSE");
		sqry.addFilterQuery("QUESTION_s:*" + alt + "* OR QUESTION_ALT_ms:*" + alt + "*");
		sqry.addSort("kid_l", SolrQuery.ORDER.asc);
		sqry.setRows(10);
		return sqry;
	}

	public static void exportQA(Tenant tenant, String type, Long runTime, int start) {

		String[] header = new String[] { "id", "testcase", "alt" };
		try {
			SXSSFWorkbook sxssfWorkbook = new SXSSFWorkbook(100);
			Sheet sheet = sxssfWorkbook.createSheet();
			Row sheetHeader = sheet.createRow(0);
			for (int x = 0; x < header.length; x++) {
				Cell headerCell = sheetHeader.createCell(x);
				headerCell.setCellType(CellType.STRING);
				headerCell.setCellValue(header[x]);
			}
			int rowCount = 1;

			SolrQuery q = getQuery(start);
			SolrServer server = tenant.getCoreServer4Write();
			SolrDocumentList result = server.query(q).getResults();
			QAUtil qautil = QAUtil.getInstance(tenant.getId());
			for (int i = 0, j = result.size(); i < j; i++) {
				Map<String, Object> docMap = new HashMap<String, Object>();

				SolrDocument doc = result.get(i);
				List<String> qaAltTpls = (List) doc.getFieldValues("QUESTION_ALT_TPL_ms");
				String currentId = (String) doc.getFieldValue("id");
				for (String qaAltTpl : qaAltTpls) {
					Row row = sheet.createRow(rowCount);
					Map<String, String> altData = QA.parseQAAlt(qaAltTpl);
					String tc = StringUtils.trimToEmpty(altData.get("testCase"));
					if (false) {
						for (int x = 0; x < header.length; x++) {
							Cell headerCell = row.createCell(x);
							headerCell.setCellType(CellType.STRING);
							String nlpColor = QAUtil.nlpWithColor(qautil, tc, Locale.TAIWAN);

							switch (x) {
							case 1:
								headerCell.setCellValue(tc);
								break;
							case 2:
								headerCell.setCellValue(nlpColor);
								break;
							}
						}
					} else {
						String alt = StringUtils.trimToEmpty(altData.get("alt"));
						Matcher m = Pattern.compile("\\(([^\\)]+)\\)").matcher(alt);
						List<String> alts = new ArrayList<String>();
						while (m.find()) {
							alts.add(m.group(1));
						}

						Map<Integer, List<String>> r = alts.size() > 4 ? PermSwap.doit(alts, 4, true)
								: new TreeMap<Integer, List<String>>();
						String altStr = "";
						String partStr = "";
						for (Integer key : r.keySet()) {
							partStr = "";
							for (String str : r.get(key)) {
								partStr += str;
							}
							DictionaryDatabase[] currentKPs = KnowledgePointDictionary.search(tenant.getId(),
									partStr.toCharArray(), null);
							if (currentKPs != null && currentKPs.length > 0) {
								SolrDocumentList results = server.query(getAltQuery(partStr)).getResults();
								if (results.size() == 0) {
									altStr += partStr + "\n";
								} else {
									boolean repeat = false;
									for (int k = 0, l = results.size(); k < l; k++) {
										String matchId = (String) results.get(k).getFieldValue("id");
										if (!matchId.equals(currentId)) {
											repeat = true;
										}
									}
									if (!repeat) {
										altStr += partStr + "\n";
									}
								}
							}
						}

						for (int x = 0; x < header.length; x++) {
							Cell headerCell = row.createCell(x);
							headerCell.setCellType(CellType.STRING);

							switch (x) {
							case 1:
								headerCell.setCellValue(alt);
								break;
							case 2:
								headerCell.setCellValue(altStr);
								break;
							}
						}
					}
					Cell headerCell = row.createCell(0);
					headerCell.setCellType(CellType.STRING);
					headerCell.setCellValue(currentId);
					rowCount++;
				}
			}
			sxssfWorkbook.write(new FileOutputStream(WiSeEnv.getHomePath().replace("kernel", "webapps") + File.separator
					+ "wise" + File.separator + "commons" + File.separator + runTime + "-" + (start + 10) + ".xlsx"));
			sxssfWorkbook.dispose();
		} catch (Exception ignore) {
			ignore.printStackTrace();
		}
	}

}