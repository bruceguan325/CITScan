package com.intumit.solr.robot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext.MenuView;
import com.intumit.solr.robot.QAContext.Option;
import com.intumit.solr.robot.QAContext.OptionMenu;
import com.intumit.solr.robot.QAUtil.FormalAnswerReplacer;
import com.intumit.solr.robot.QAUtil.FormalAnswerStickerReplacer;
import com.intumit.solr.robot.function.FunctionUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intumit.citi.CitiUtil;
import com.intumit.citi.frontend.*;

public class RichTextAnswerTransformer extends WebRichMessageAnswerTransformer implements AnswerTransformer {

	@Override
	public QAContext transform(QAChannel ch, QAContext ctx) {
		StringBuilder answerText = ctx.answerText;
		String NL = ctx.isClientSupportHtml() ? "<br>" : "\n";
		JSONArray messages = new JSONArray();
		int opNum = 1;
		ArrayList<Map<String, Object>> optionDataList = new ArrayList<Map<String, Object>>();

		if (answerText == null)
			answerText = new StringBuilder();

		try {
			MessageButtons msgbtn = null;
			MessageText msgtxt = new MessageText();
			JSONObject jsonobj = (JSONObject) ctx.getCtxAttr("_bundle");
			msgtxt.setId("");// jsonobj.has("id")?jsonobj.get("id").toString():"");
			msgtxt.setType(Message.Type.TEXT);

			String Output = answerText.toString();

			Output = FunctionUtil.collectExecAndReplace(Output, ctx);
			FormalAnswerReplacer far = QAUtil.getInstance(ctx.getTenant()).getFormalAnswerReplacer(ctx);
			Output = TemplateUtil.processTwice(ctx, Output, far);
			/*
			 * FormalAnswerStickerReplacer farSticker =
			 * QAUtil.getInstance(ctx.getTenant()).getFormalAnswerStickerReplacer(ctx);
			 * Output = TemplateUtil.process(ctx, Output, farSticker); Output =
			 * TemplateUtil.process(ctx, Output, QAUtil.getCleaner());
			 */
			Pattern pat = Pattern.compile("(<li.+?)?(<a\\s.+?a>)");
			Matcher matcher = pat.matcher(Output); // 將標準問答轉成迪普文字客服的樣式
			StringBuilder strbu = new StringBuilder();
			int pos = 0;
			String data_id = "";
			boolean hasli = false;
			while (matcher.find()) {
				strbu.append(Output.substring(pos, matcher.start()));
				if (matcher.group(2).contains("href=\"tel:")) // 花旗立即申請
				{
					hasli = true;
					data_id = matcher.group(2).replaceAll("href=\"(tel:.+?)\"", "data-id=\"$1\" href=\"javascript:;\"");
					data_id = data_id.replace("class=\"", "class=\"phone ");
					strbu.append(matcher.group(1).replace("<li",
							"<li class=\"btnPhone\" "
									+ data_id.substring(data_id.indexOf("data-id"), data_id.indexOf(" href"))
									+ " data-l=\"" + ctx.getCurrentQuestion() + "-撥打\""));
					strbu.append(data_id.replaceFirst("data-id.+? (href)", "$1"));
				} else if (matcher.group(2).contains("${transfer}")) // 花旗客服即時通
				{
					hasli = true;
					data_id = matcher.group(2).replaceAll("href=\"https?://(.+?)\"",
							"data-id=\"$1\" href=\"javascript:;\"");
					strbu.append(matcher.group(1).replace("<li", "<li class=\"btnTransfer\" "
							+ data_id.substring(data_id.indexOf("data-id"), data_id.indexOf(" href"))));
					strbu.append(data_id.replaceFirst("data-id.+? (href)", "$1"));
				} else if (matcher.group(2).contains("http")) // 花旗超連結彈出視窗
				{
					if (StringUtils.isNotBlank(matcher.group(1))) {
						hasli = true;
						data_id = matcher.group(2).replaceAll("href=\"(https?://.+?)\"",
								"data-id=\"$1\" href=\"javascript:;\"");
						strbu.append(matcher.group(1).replace("<li",
								"<li class=\"btnRedir\" "
										+ data_id.substring(data_id.indexOf("data-id"), data_id.indexOf(" href"))
										+ " data-l=\"" + ctx.getCurrentQuestion() + "-立即前往\""));
						strbu.append(data_id.replaceFirst("data-id.+? (href)", "$1"));
					} else {
						strbu.append(matcher.group(2)
								.replaceAll("href=\"(https?://.+?)\"", "data-id=\"$1\" href=\"javascript:;\"")
								.replaceAll("class=\"(.+?)\"", "class=\"$1 btnRedir\""));
					}
				} else
					strbu.append(Output.substring(matcher.start(), matcher.end()));
				pos = matcher.end();
			}
			if (strbu.length() > 0) {
				strbu.append(Output.substring(pos));
				Output = strbu.toString();
				strbu.setLength(0);
			}
			// 花旗有些答案寫在 extraParams 裡面
			if (ctx.response.containsKey("Messages"))// || ctx.response.containsKey("Box"))
				msgtxt.setText(Output);
			else {
				msgtxt.setText(
						(hasli ? "<div class=\"talk_btns\"><div class=\"talk_box\">" : "<div class=\"talk_box\">")
								+ Output.replace("<ul>", "</div><div class=\"btn_item center\"><ul>")// .replaceAll("(<img.+?src=\").+?ckeditor-upload.+?(/[0-9]+?\\.(jpg|png|bmp)\")",
																										// "$1\\${imageurl}$2")
								+ (hasli ? "</div></div>" : "</div>"));
			}
			System.out.println(Output);
			ObjectMapper mapper = new ObjectMapper();
			String jsonInString = mapper.writeValueAsString(msgtxt);
			ctx.response.put("Output", new JSONObject(jsonInString));

			if (!ch.getSupportMultiRichMessages()) {
				if (ctx.getShowedOptionMenuSize() > 0) {
					msgbtn = new MessageButtons();
					msgbtn.setId("");// jsonobj.has("id")?jsonobj.get("id").toString():"");
					msgbtn.setType(Message.Type.BUTTONS);
					msgbtn.setText(RobotFormalAnswers.getAnswers(ctx.getTenant().getId(), "youMayWannaKnow").get(0)
							.toString());

					for (String key : ctx.showedOptionMenuKeys) {
						OptionMenu menu = ctx.optionMenuMap.get(key);
						if (ch.getAppendOptionToOutput()) {
							if (answerText.length() > 0) {
								answerText.append(NL);
							}
							answerText.append(menu.menuHeader);
						}

						if (menu.view != MenuView.HIDDEN) {
							for (Option option : menu.options) {
								int currentOpNum = opNum++;
								boolean hasAnswer = StringUtils.isNotBlank(option.getValue().answer);
								if (ch.getReturnOptionInJson()) {
									Map<String, Object> optionData = new HashMap<String, Object>();
									if (option.value != null && option.value.question != null) // 有 question 用 question
										optionData.put("option", option.value.question);
									else
										optionData.put("option", "" + currentOpNum); // 沒 question 用編號
									optionData.put("title", option.getName());
									optionData.put("eventSource", menu.getCreateFrom());
									optionData.put("eventSourceType", QAContext.EVENT_SOURCE_TYPE_BUTTON);
									optionDataList.add(optionData);
									if (!jsonobj.has("testMode") || !jsonobj.getString("testMode").equals("true")) {
										if (hasAnswer && option.getValue().answer.startsWith("http")) {
											msgbtn.setText("");
											msgbtn.addAction(new Action(Action.Type.URL, option.getName(),
													option.getValue().answer, "" + currentOpNum));
										} else // 把附加階層轉給迪普文字客服
										{
											if (strbu.length() == 0)
												strbu.append(CitiUtil.getMyLink()).append("/qa-ajax.jsp?apikey=")
														.append(jsonobj.getString("apikey"))
														.append(jsonobj.getString("id").length() <= 10
																? "&id=" + jsonobj.getString("id")
																: "")
														.append(jsonobj.has("UserID")
																? "&UserID=" + jsonobj.getString("UserID")
																: "")
														.append("&q=");
											msgbtn.addAction(new Action(Action.Type.URL, option.getName(),
													(strbu.toString() + option.getName()), "" + currentOpNum));
										}
									}
								}

								if (ch.getAppendOptionToOutput()) {
									if (ctx.isClientSupportHtml()) {
										answerText.append(menu.indentOrSplitString
												+ "{{F:ENTER}}<span class='question-cmd'>" + (currentOpNum)
												+ "</span>.<span class='question-clickable other-questions-li'"
												+ " data-es='" + menu.getCreateFrom() + "'" + " data-est='"
												+ QAContext.EVENT_SOURCE_TYPE_BUTTON + "'" + " data-opnum='"
												+ option.getName() + "'>" + option.getName() + "</span>");

										if (ctx.getTenant().getEnableDebug()) {
											Long kid = option.getRefToKid();

											if (kid != null) {
												String href = QAUtil.getContextPath()
														+ "/wiseadm/qaDataEditor.jsp?id=COMMON_SENSE-" + kid;
												String a = "&nbsp;&nbsp;<a href='" + href + "' target='"
														+ QAUtil.ANSWER_LINK_TARGET + "'>(<bean:message key='num'/>"
														+ kid + ")</a>";
												answerText.append(a);
											}
										}

										answerText.append(NL);
									} else {
										answerText.append(NL + menu.indentOrSplitString + "{{F:ENTER}}" + (currentOpNum)
												+ "." + option.getName());
									}
								}
							}
						}
						answerText.append(menu.menuFooter);
						answerText = QAUtil.replaceDoubleWraps(answerText.toString(), NL + NL, NL);
					}
				}
			} else {
				// 支援多圖文，除了原始內容之外，選單的東西全部都轉成圖文
				generateRichMessage(ctx, messages, ch.getUseHtmlNewline());
				processOptionMenus(ctx, messages, ch.getUseHtmlNewline());
			}

			if (ch.getReturnOptionInJson() && !optionDataList.isEmpty()) {
				try {
					answerText = QAUtil.replaceDoubleWraps(answerText.toString(), NL + NL, NL);
					if (ctx.getResponseAttribute("options") != null) {
						JSONArray jarray = (JSONArray) ctx.getResponseAttribute("options");
						jarray.addAll(optionDataList);
						ctx.setResponseAttribute("options", jarray);
					} else {
						ctx.setResponseAttribute("options", new JSONArray(optionDataList));
					}
					if (null != msgbtn) {
						// ObjectMapper mapper = new ObjectMapper();
						jsonInString = mapper.writeValueAsString(msgbtn);
						ctx.response.put("Options", new JSONObject(jsonInString));
					}
				} catch (Exception e) {
					System.out.println(e);
				}
			}
		} catch (JSONException | JsonProcessingException e) {
			e.printStackTrace();
		}

		String output = answerText.toString();
		output = FunctionUtil.collectExecAndReplace(output, ctx);
		FormalAnswerReplacer far = QAUtil.getInstance(ctx.getTenant()).getFormalAnswerReplacer(ctx);
		output = TemplateUtil.processTwice(ctx, output, far);

		FormalAnswerStickerReplacer farSticker = QAUtil.getInstance(ctx.getTenant())
				.getFormalAnswerStickerReplacer(ctx);
		output = TemplateUtil.process(ctx, output, farSticker);
		output = TemplateUtil.process(ctx, output, QAUtil.getCleaner());

		ctx.setAnswerText(output);

		try {
			if (ch.getSupportMultiRichMessages()) {

				// 這裡直接把 answerText 塞在 messages 第一個，不再等到 conn-line.jsp 當中才去 insert
				if (output.length() > 0) {
					JSONObject msgObj = new JSONObject();
					try {
						msgObj.put("type", "html");
						msgObj.put("html", output);

						messages.add(0, msgObj);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			if (messages.length() > 0) {
				ctx.response.put("webRM", new JSONObject().put("messages", messages));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return ctx;
	}

}
