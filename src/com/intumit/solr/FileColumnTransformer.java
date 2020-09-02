package com.intumit.solr;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.Transformer;

import com.intumit.solr.util.FileParser;

/**
 * 資料庫的欄位如果是一個文字檔案的路徑。
 * 透過此 Transformer 可以讀取該檔並放入特定欄位中 * 
 */
public class FileColumnTransformer extends Transformer {

	/**
	 * 代表這個欄位是一個實體路徑
	 */
	private static final String PATH = "fileCol";
	private static final String BASE_PATH = "basePath";
	private static final String DEST = "destColName";
	private static final String DELETE_AFTER_TRANSFORMED = "deleteAfterTransformed";
	
	/**
	 * 用分號分開，不用加上萬用字元，例如 ".doc;.html;.htm;.php"
	 */
	private static final String EXTENSION = "extension";

	@SuppressWarnings("unchecked")
	public Object transformRow(Map<String, Object> row, Context context) {

		for (Map<String, String> map : context.getAllEntityFields()) {
			String doReadFile = map.get(PATH);
			String destColName = map.get(DEST);
			final String supportedExtension = map.get(EXTENSION);
			String delAfter = map.get(DELETE_AFTER_TRANSFORMED);
			
			if (doReadFile == null)
				continue;
			
			if (destColName == null) {
				System.out.println("Please assign dest column name.");
				continue;
			}
			
			String basePath = map.get(BASE_PATH);

			if (new Boolean(doReadFile)) {
				String key = map.get(DataImporter.COLUMN);
				Object value = row.get(key);
				if (value instanceof String) {
					String str = (String) value;
					if (str != null) {
						try {
							File file = new File(StringUtils.trimToEmpty(basePath) + str);
							StringBuffer text = new StringBuffer();
							recursive(file, text, supportedExtension);
							row.put(destColName, text.toString());

							if (file != null && delAfter != null && new Boolean(delAfter)) {
								file.delete();
							}
						} 
						catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		return row;
	}
	
	/*public static void main(String[] args) {
		File dir = new File("./intumit/src");
		StringBuffer buf = new StringBuffer();
		
		recursive(dir, buf, ".java");
		
		try {
			IOUtils.write(buf, new FileWriter(new File("./output2.txt")));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}*/
	
	private static void recursive(File file, StringBuffer strBuf, final String extensions) {
		if (file.exists()) {
			if (file.isDirectory()) {
				
				// 先列出所有非目錄以及符合 extension 要求的檔案
				File[] files = file.listFiles(new FileFilter() {

					@Override
					public boolean accept(File pathname) {

						if (extensions == null) {
							return true;
						}
						
						if (pathname.isDirectory())
							return false;
						
						String fileName = pathname.getName();
						if (fileName.lastIndexOf(".") != -1) {
							String ext = fileName.substring(fileName.lastIndexOf("."));
							
							if (extensions.indexOf(ext) != -1) {
								return true;
							}
						}
						return false;
					}
					
				});

				FileParser parser = FileParser.getInstance();
				for (int i=0; i < files.length; i++) {
//					System.out.println("--- FILE：" + files[i].getName() + " ---");
//					strBuf.append("\r\n\r\n--- FILE：" + files[i].getName() + " ---\r\n");
					strBuf.append(parser.autoParse(files[i]));
				}
				
				// recursive 到每個目錄當中
				File[] dirs = file.listFiles(new FileFilter() {

					@Override
					public boolean accept(File pathname) {						
						if (pathname.isDirectory())
							return true;
						
						return false;
					}
					
				});
				
				for (int i=0; i < dirs.length; i++) {
					recursive(dirs[i], strBuf, extensions);
				}
			}
		}
	}
	

	/**
	 * @deprecated 
	 * @param path
	 * @param encoding
	 * @return
	 */
	private static String readFile(String path, String encoding) {
		File file = new File(path);
		if (file.exists()) {
			try {
				return FileUtils.readFileToString(file, encoding);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("File not found:" + path);
		}
		return null;
	}
}
