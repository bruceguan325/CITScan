package com.intumit.solr.robot;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.intumit.solr.util.WiSeEnv;
import com.intumit.solr.util.WiSeUtils;

public class MenuImageCreator {

	private static String WEB_CACHE_DIR = null;
	
	
	public static boolean isGoodForImageMap(List<String> menus) {
		int maxWord = 0;
		for (String m: menus) {
			maxWord = Math.max(maxWord, m.length());
		}
		
		return maxWord <= 8;
	}
	
	public static JSONObject create(List<String> menus, String imageFormat, String bgColor, String fontColor, String boxBgColor, String boxBorderColor, int borderStroke) {
		StringBuffer buf = new StringBuffer(bgColor + fontColor + boxBgColor + boxBorderColor + borderStroke);
		for (String m: menus) {
			buf.append(m);
		}
		String cacheKey = WiSeUtils.sha256(buf.toString());
		if (WEB_CACHE_DIR == null) {
			WEB_CACHE_DIR = WiSeEnv.getHomePath().replace("kernel", "webapps") + File.separator + "wise"
					+ File.separator + "commons" + File.separator + "imagemaps" + File.separator ;
		}
		File dir = new File(WEB_CACHE_DIR);
		if (!dir.exists()) dir.mkdirs();
		
		File cacheImageDir = new File(WEB_CACHE_DIR + cacheKey);
		if (!cacheImageDir.exists()) cacheImageDir.mkdirs();
		
		File cache1040ImageMap = new File(WEB_CACHE_DIR + cacheKey + File.separator + "1040.json");
		
		JSONObject result = null;
		
		try {
			if (cache1040ImageMap.exists()) {
				result = new JSONObject(FileUtils.readFileToString(cache1040ImageMap, "UTF-8"));
			}
			else {
				result = new JSONObject();
        			JSONArray menusMap = new JSONArray();
        			result.put("map", menusMap);
        			
        			// you can pass in fontSize, width, height via the request
        			int cols = 3;
        			int rows = 1;
        			int maxWord = 0;
        			for (String m: menus) {
        				maxWord = Math.max(maxWord, m.length());
        			}
        			
        			if (maxWord <= 5) {
        				cols = 3;
        			}
        			else {
        				cols = 2;
        			}
        			
        			int fontSize = maxWord <= 5 ? 56 : 48;
        			
        			rows = (int)Math.ceil( ((double)menus.size()) / cols);
        
        			int borderArc = 50;
        			Color backgroundColor = Color.decode(bgColor);
        			Color boxColor = Color.decode(boxBgColor);
        			Color borderColor = Color.decode(boxBorderColor);
        			Color textColor = Color.decode(fontColor);
        			Font textFont = new Font(getGoodFontName(), Font.PLAIN, fontSize);
        			int boxW = (int)Math.ceil(1040d / cols);
        			int boxH = 90;
        			int width = 1040;
        			int height = boxH * rows + 10;
        			int boxPadding = 10;
        			int marginTop = 10;
        			int marginLeft = 5;
        			float imageQuality = 0.95f; // max is 1.0 (this is for jpeg)
        			
        			result.put("width", width);
        			result.put("height", height);
        			
        			BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        			Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
        			g.setRenderingHint( // 文字反鋸齒，不然 Linux 下慘不忍睹
        			        RenderingHints.KEY_TEXT_ANTIALIASING,
        			        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        			g.setColor(backgroundColor);
        			g.fillRect(0, 0, width, height);
        			g.setColor(textColor);
        			g.setFont(textFont);
        
        			for (int i=0; i < rows; i++) {
        				for (int j=0; j < cols; j++) {
        					int offset = i * cols + j;
        					if (offset < menus.size()) {
        						try {
        							JSONObject o = new JSONObject();
        							Stroke oldStroke = g.getStroke();
        							g.setStroke(new BasicStroke(borderStroke));
        							g.setColor(boxColor);
        							g.fillRoundRect(j*boxW + marginLeft, i*boxH + marginTop, boxW-boxPadding, boxH-boxPadding, borderArc, borderArc);
        							g.setColor(borderColor);
        							g.drawRoundRect(j*boxW + marginLeft, i*boxH + marginTop, boxW-boxPadding, boxH-boxPadding, borderArc, borderArc);
        							g.setStroke(oldStroke);
        							o.put("x", j*boxW);
        							o.put("y", i*boxH);
        							o.put("width", boxW-boxPadding);
        							o.put("height", boxH-boxPadding);
        
        							String m = menus.get(offset);
        							o.put("title", m);
        							FontMetrics fm = g.getFontMetrics();
        							int sw = fm.stringWidth(m);
        							
        							int x = j * boxW + ((boxW - sw) / 2) + marginLeft;
        							int y = (i+1) * boxH - ((boxH-fontSize)/2) - boxPadding + marginTop;
        							
        							System.out.println("" + m + " (" + x + ", " + y + ")");
        							g.drawString(m, x, y);
        							menusMap.put(o);
        						}
        						catch (JSONException e) {
        							e.printStackTrace();
        						}
        					}
        				}
        			}

        			result.put("imageFilename", cacheKey);
        			ByteArrayOutputStream bo = createImageByteArray(bufferedImage, imageFormat, imageQuality);
        			
        			// 固定先產生 1040 版本
    				FileUtils.writeStringToFile(cache1040ImageMap, result.toString(4), "UTF-8");
        			FileUtils.writeByteArrayToFile(new File(WEB_CACHE_DIR + cacheKey + File.separator + "1040"), bo.toByteArray());
        			
        			// 然後依序產生較低解析度的版本
        			int[] allImageWidth = new int[] {700, 460, 300, 240};
        			
        			for (int newW: allImageWidth) {
        				int newH = (int) (Math.ceil((double) height) * (newW / 1040d));
        				BufferedImage newBufImg = resizeImageWithHint(bufferedImage, newW, newH, bufferedImage.getType());
        				ByteArrayOutputStream newBo = createImageByteArray(newBufImg, imageFormat, imageQuality);
        				JSONObject newResult = resizeImageMapJson(result, newW);
        				
        				FileUtils.writeStringToFile(new File(WEB_CACHE_DIR + cacheKey + File.separator + newW + ".json"), newResult.toString(4), "UTF-8");
            			FileUtils.writeByteArrayToFile(new File(WEB_CACHE_DIR + cacheKey + File.separator + newW), newBo.toByteArray());
        			}
        
        			g.dispose();
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return result;
	}
	
	private static JSONObject resizeImageMapJson(JSONObject result, int newW) {
		JSONObject nr = null;
		try {
			int width = result.getInt("width");
			int height = result.getInt("height");
			double rate = (newW / (double) width);
        		int newH = (int) (Math.ceil(height * rate));
        		nr = new JSONObject(result.toString());
        		
        		nr.put("width", newW);
        		nr.put("height", newH);
        		
        		JSONArray mapArr = nr.getJSONArray("map");
        		
        		for (int i=0; i < mapArr.length(); i++) {
        			JSONObject cell = mapArr.getJSONObject(i);
        			
        			int cw = (int) Math.ceil(cell.getInt("width") * rate);
        			int ch = (int) Math.ceil(cell.getInt("height") * rate);
        			int cx = (int) Math.ceil(cell.getInt("x") * rate);
        			int cy = (int) Math.ceil(cell.getInt("y") * rate);
        			
        			cell.put("width", cw);
        			cell.put("height", ch);
        			cell.put("x", cx);
        			cell.put("y", cy);
        		}
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return nr;
	}

	private static ByteArrayOutputStream createImageByteArray(BufferedImage bufferedImage, String imageFormat, float imageQuality) {

		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		try {
			Iterator iter = ImageIO.getImageWritersByFormatName(imageFormat);
			if (iter.hasNext()) {
				ImageWriter writer = (ImageWriter) iter.next();
				ImageWriteParam iwp = writer.getDefaultWriteParam();
				if (imageFormat.equalsIgnoreCase("jpg")
						|| imageFormat.equalsIgnoreCase("jpeg")) {
					iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					iwp.setCompressionQuality(imageQuality);
				}
				writer.setOutput(ImageIO.createImageOutputStream(bo));
				IIOImage imageIO = new IIOImage(bufferedImage, null, null);
				writer.write(null, imageIO, iwp);

			} else {
				throw new RuntimeException("no encoder found for jsp");
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return bo;
	}

	/**
	 * 尋找合適的字體，Linux 可能要找 TTF（TrueType Font）類的字體
	 * 這裡還沒有考慮不同語系的時候會有問題
	 * 
	 * @return
	 */
	public static String getGoodFontName() {
		Set<String> fontFamilies = new HashSet<>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
		String[] preferFonts = new String[] {"Gen Shin Gothic Regular", "微軟正黑體", "黑體", "Gen Jyuu Gothic Medium", "宋體", "楷體"};
		for (String f: preferFonts) {
			if (fontFamilies.contains(f)) {
				return f;
			}
		}
		
		return Font.SANS_SERIF;
	}
	
	private static BufferedImage resizeImage(BufferedImage originalImage, int w, int h, int type) {
		BufferedImage resizedImage = new BufferedImage(w, h, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, w, h, null);
		g.dispose();

		return resizedImage;
	}

	private static BufferedImage resizeImageWithHint(BufferedImage originalImage, int w, int h, int type) {

		BufferedImage resizedImage = new BufferedImage(w, h, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, w, h, null);
		g.dispose();
		g.setComposite(AlphaComposite.Src);

		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		return resizedImage;
	}

	public static void main(String[] args) throws JSONException {
		String imageFormat = "jpg";
		List<String> menus = Arrays.asList(new String[] {"美元", "人民幣", "港幣", "日圓", "歐元", "澳幣", "英鎊", "加拿大幣", "紐西蘭幣", "瑞士法郎", "南非幣", "新加坡幣", "泰銖", "墨西哥披索", "瑞典幣"});
		create(menus, imageFormat, "#FFFFFF", "#3B5998", "#FFFFFF", "#3B5998", 5);
	}

}
