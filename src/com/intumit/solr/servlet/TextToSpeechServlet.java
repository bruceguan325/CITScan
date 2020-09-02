package com.intumit.solr.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.intumit.hithot.HitHotLocale;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContextManager;
import com.microsoft.ttshttpoxford.sample.AudioOutputFormat;
import com.microsoft.ttshttpoxford.sample.Gender;
import com.microsoft.ttshttpoxford.sample.TTSService;

//this to be used with Java Servlet 3.0 API
@MultipartConfig
public class TextToSpeechServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/***************************************************
	 * URL: /upload doPost(): upload the files and other parameters
	 ****************************************************/
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		doGet(request, response);
	}
	
	/***************************************************
	 * URL: /upload?f=value doGet(): get file of index "f" from List<FileMeta>
	 * as an attachment
	 ****************************************************/
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		try {
			// TODO Auto-generated method stub
			String[] ttsSetting = TTSService.getMicrosoftTtsSetting(HitHotLocale.zh_TW);
			String qaId = request.getParameter("qaId");
			String textToSynthesize = null;
			if (qaId != null) {
				QAContext qaCtx = QAContextManager.lookup(qaId);
				if (qaCtx != null) {
					textToSynthesize = qaCtx.getAnswerText().toString();
					
					if (qaCtx.getTenant() != null) {
						HitHotLocale locale = qaCtx.getTenant().getLocale();
						String[] newTtsSetting = TTSService.getMicrosoftTtsSetting(locale);
						if (newTtsSetting != null) {
							ttsSetting = newTtsSetting;
						}
					}
				}
			}
			else {
				textToSynthesize = StringUtils.defaultString(request.getParameter("pronounce"), textToSynthesize);
			}
			

			if (StringUtils.isNotBlank(textToSynthesize)) {
				String outputFormat = AudioOutputFormat.Riff16Khz16BitMonoPcm;
				String deviceLanguage = ttsSetting[0];
				String genderName = ttsSetting[1];
				String voiceName = ttsSetting[2];
				byte[] audioBuffer = null;
				int retry = 3;

				if (StringUtils.startsWith(textToSynthesize, "抱歉~您的問題不是很明確，請更具體描述您的問題")) {
					textToSynthesize = "抱歉~您的問題不是很明確，請更具體描述您的問題";
				}
				else {
					textToSynthesize = ttsClean(textToSynthesize, true, true);
				}

				for (String stopWord: new String[] {"。", "！", "\n"}) {
					if (textToSynthesize.length() > 200) {
						int len = textToSynthesize.length();
						textToSynthesize = StringUtils.left(textToSynthesize, 200) + StringUtils.substringBeforeLast(StringUtils.substring(textToSynthesize, 200), stopWord);
						
						if (len != textToSynthesize.length()) {
							break;
						}
					}
				}

				while (retry-- >= 0) {
					try {
						audioBuffer = TTSService.Synthesize(textToSynthesize, outputFormat, deviceLanguage, genderName, voiceName);

						if (audioBuffer != null) break;
					}
					catch (Exception e) {
						if (retry >= 0) System.out.println("" + retry + " retries left:" + e.getMessage());
						else e.printStackTrace();
					}
				}

				if (audioBuffer != null) {
					OutputStream output = response.getOutputStream();
					String key = TTSService.getCacheFileName(textToSynthesize, deviceLanguage, genderName);
					output.write(key.getBytes());
					output.close();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static String ttsClean(String org, boolean doVoiceOptimization, boolean removeRecommendation) {
		String text = org;
		if (StringUtils.startsWith(text, "抱歉~您的問題不是很明確，請更具體描述您的問題")) {
			text = "抱歉~您的問題不是很明確，請更具體描述您的問題";
		}
		else {
			text = text.replaceAll("(?is)<div[^>]+disable-voice[^>]+>.*?</div>", "");
			text = text.replaceAll("<table[^>]*>.*?</table>", " ");
			text = text.replaceAll("<[^>]+>", " ");
			text = text.replaceAll("\\&nbsp;", "");
			text = text.replaceAll("[\\:：\\|]", ", ");

			if (removeRecommendation) {
				text = text.replaceAll("另外，關於您的提問.*", "");
			}

			if (doVoiceOptimization) {
				text = StringUtils.replaceEach(text, new String[] { "０", "１", "２", "３", "４", "５", "６", "７", "８", "９" },
						new String[] { "零", "一", "二", "三", "四", "五", "六", "七", "八", "九" });
				text = text.replaceAll("（.*?）", "");
				text = text.replaceAll("\\(.*?\\)", "");
				text = text.replaceAll("([0-9]*\\.?[0-9]+)%", "百分之$1");
				text = text.replaceAll("行庫", "航庫");
				text = text.replaceAll("還款", "環款");
				text = text.replaceAll("是那裡", "是哪裡");
				text = text.replaceAll("返還", "返環");
				text = text.replaceAll("為", "圍");
				text = text.replaceAll("圍(?=什麼|您)", "為");
				text = text.replaceAll("->", "。");
			}
		}

		return text;
	}
}
