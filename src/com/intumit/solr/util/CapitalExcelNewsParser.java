package com.intumit.solr.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import com.hankcs.hanlp.dictionary.ts.BaseChineseDictionary;

public class CapitalExcelNewsParser {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			parseAndSave(new FileInputStream(new File("sample/Capital/新聞標題彙整摘要範本-2i-Herb.xlsm")), new FileOutputStream("output.xlsm"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static void parseAndSave(InputStream uploaded, OutputStream modified) {

		try
		{
			//Create Workbook instance holding reference to .xlsx file
			XSSFWorkbook workbook = new XSSFWorkbook(uploaded);

			//Get first/desired sheet from the workbook
			XSSFSheet sheet = workbook.getSheetAt(1);

			//Iterate through each rows one by one
			Iterator<Row> rowIterator = sheet.iterator();
			while (rowIterator.hasNext()) 
			{
				try {
					Row row = rowIterator.next();
					//For each row, iterate through all the columns
					Iterator<Cell> cellIterator = row.cellIterator();
					if (row.getLastCellNum() < 3) {
						continue;
					}
					Cell cell1 = row.getCell(0);
					Cell cell2 = row.getCell(1);
					Cell cell3 = row.getCell(2);
					
					//Check the cell type and format accordingly
					String title = cell3.getStringCellValue();

					if (StringUtils.trimToNull(title) == null)
						continue;
					
					Stock[] stocks = StockDictionary.search(title.toCharArray());
					System.out.print(title);
					if (stocks.length > 0) {
						System.out.println("*******" + stocks[0]);
						cell1.setCellValue(stocks[0].code);
						cell2.setCellValue(stocks[0].name);
					}

					System.out.println("");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			workbook.write(modified);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	public static class Stock {
		String name;
		String code;
		
		@Override
		public String toString() {
			return "Stock [name=" + name + ", code=" + code + "]";
		}
	}

	public static class StockDictionary extends BaseChineseDictionary
	{
		static AhoCorasickDoubleArrayTrie<Stock> trieInstance;
	
	    public static void clear(Integer tenantId) {
	    	trieInstance = null;
	    }
	    
	    public static AhoCorasickDoubleArrayTrie<Stock> loadIfNull() {
	    	if (trieInstance == null) {
	    		AhoCorasickDoubleArrayTrie<Stock> trie = new AhoCorasickDoubleArrayTrie<Stock>();
		        TreeMap<String, Stock> map = new TreeMap<String, Stock>();
		        List<Stock> ddList = loadStockFromCsv();
		        
		        for (Stock dd: ddList) {
		        	map.put(dd.name, dd);
		        }
	
			    if (map.size() == 0) return null;
			    
		        trie.build(map);
		        
		        trieInstance = trie;
	    	}
	    	
	    	return trieInstance;
	    }
	
	    public static Stock[] search(char[] charArray) {
	    	final List<Stock> results = new ArrayList<Stock>();
	    	AhoCorasickDoubleArrayTrie<Stock> trie = loadIfNull();
	    	if (trie == null) return results.toArray(new Stock[0]);
	    	
	        final Stock[] wordNet = new Stock[charArray.length];
	        final int[] lengthNet = new int[charArray.length];
	        
	        trie.parseText(charArray, new AhoCorasickDoubleArrayTrie.IHit<Stock>()
	        {
	            @Override
	            public void hit(int begin, int end, Stock value)
	            {
	                int length = end - begin;
	                if (length > lengthNet[begin])
	                {
	                    wordNet[begin] = value;
	                    lengthNet[begin] = length;
	                }
	            }
	        });
	        
	        for (int offset = 0; offset < wordNet.length; )
	        {
	            if (wordNet[offset] == null) {
	                ++offset;
	                continue;
	            }
	            else {
	            	results.add(wordNet[offset]);
	            	offset += lengthNet[offset];
	            }
	        }
	        return results.toArray(new Stock[0]);
	    }
	    
	}
	
	public static String stockSynonymsCsvPath = "sample/Capital/同義詞批次匯入範本檔案_一般常用.csv";

	public static List<Stock> loadStockFromCsv() {
		List<Stock> stocks = new ArrayList<Stock>();

		final CellProcessor[] allProcessors = null;
		ICsvListReader listReader = null;

		try {
			InputStream input = new FileInputStream(stockSynonymsCsvPath);
			String charset = com.intumit.solr.util.WiSeUtils.autoDetectCharset(input);
			input.close();
			input = new FileInputStream(stockSynonymsCsvPath);
			listReader = new CsvListReader(new InputStreamReader(input, "CP950"), CsvPreference.EXCEL_PREFERENCE);
			
			List<String> cells = null;
			try {
				while ((cells = listReader.read()) != null) {
					if (cells.size() > 1) {
						Stock s = new Stock();
						s.name = cells.get(0);
						s.code = StringUtils.strip(cells.get(1), ",");
						stocks.add(s);
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return stocks;
	}
}
