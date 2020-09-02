package com.intumit.solr.robot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Blob;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrDocument;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.hibernate.Hibernate;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.intumit.message.MessageUtil;
import com.intumit.solr.admin.AdminGroup;
import com.intumit.solr.admin.AdminGroupFacade;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.robot.QAContext.MenuSelectionBehavior;
import com.intumit.solr.robot.QAContext.MenuView;
import com.intumit.solr.robot.QAContext.OptionAction;
import com.intumit.solr.robot.dictionary.CustomDataDictionary;
import com.intumit.solr.robot.qaplugin.HierarchicalQA;
import com.intumit.solr.robot.qaplugin.ImportHierarchicalQA;
import com.intumit.solr.robot.qaplugin.QADialogPlugin;
import com.intumit.solr.robot.qaplugin.QAPlugins;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeUtils;
import com.intumit.solr.util.fileupload.FileMeta;
import com.intumit.solr.whiteKeywords.WhiteWikiWordFacade;
import com.intumit.syslog.OperationLogEntity;

//this to be used with Java Servlet 3.0 API
@MultipartConfig
public class ProcessQADataServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	public static final String QA_DATA_FILE_NAME = "智能客服問答知識庫匯入檔";
	public static final String HIERARCHICAL_QA_DATA_FILE_NAME = "智能客服階層式問答匯入檔";
	public static final String QA_DATA_MULTI_CHANNEL_ANSWER_FILE_NAME = "智能客服問答知識庫答案匯入檔";
	public static final String QA_DATA_ROBOT_CHANNEL_ANSWER_FILE_NAME = "智能客服問答知識庫Pepper答案匯入檔";
	public static final String CASUAL_DATA_FILE_NAME = "生活用語知識庫匯入檔";
	public static final String TEXT_CRM_SERVICE_LOG_DATA_FILE_NAME = "真人文字客服問答紀錄匯入檔";
	public static final String QA_EVALUATION_FILE_NAME = "智能客服測試紀錄匯入檔";

	public static List<String> trimCellsToEmpty(List<String> list) {
		int pos = list.size()-1;
		for (int i=pos; i >= 0; i--) {
			if (list.get(i) != null) {
				list.set(i, StringUtils.trimToEmpty(list.get(i)));
			}
		}

		return list;
	}
	
	public static List<String> truncateTailNulls(List<String> list) {
		int pos = list.size()-1;
		for (int i=pos; i >= 0; i--) {
			if (list.get(i) != null) {
				pos = i;
				break;
			}
		}

		return list.subList(0, pos+1);
	}

	List<String> getListMultivalue(Object value) {
		if (value instanceof List) {
			return (List<String>)value;
		}

		List<String> l = new ArrayList<String>();

		if (value != null)
			l.add(value.toString());

		return l;
	}

	public JSONObject getHQaObject(ImportHierarchicalQA imHqa) {
		JSONObject jo = new JSONObject();
		try {
			jo.put("id", imHqa.getQaid() != null ? Long.valueOf(imHqa.getQaid()) : System.currentTimeMillis());
			jo.put("optionAction", imHqa.getOptionAction() != null ? imHqa.getOptionAction() : "");
			jo.put("pipe", imHqa.getPipe() != null ? imHqa.getPipe() : "");
			jo.put("question", imHqa.getQuestion() != null ? imHqa.getQuestion() : "");
			jo.put("answer", imHqa.getAnswer() != null ? imHqa.getAnswer() : "");
			jo.put("matchSentences", imHqa.getMatchSentences() != null ? imHqa.getMatchSentences() : new JSONArray());
			jo.put("script", imHqa.getScript() != null ? imHqa.getScript() : "");
			jo.put("extraParams", imHqa.getExtraParams() != null ? imHqa.getExtraParams() : new JSONObject());
			jo.put("expiry", imHqa.getExpiry() != null ? imHqa.getExpiry() : "");
			jo.put("menuView", imHqa.getMenuView() != null ? imHqa.getMenuView() : "");
			jo.put("inputType", imHqa.getInputType() != null ? imHqa.getInputType() : "");
			jo.put("text", imHqa.getText() != null ? imHqa.getText() : "");
			jo.put("children", new JSONArray());
			Thread.sleep(1);
		} catch (Exception e) {
			System.out.println(e);
		}
		return jo;
	}

	public ImportHierarchicalQA getHQa(List<String> cells, String[] headers) {
		ImportHierarchicalQA im = new ImportHierarchicalQA();
		for (int i=0; i < headers.length && i < cells.size(); i++) {
			if(headers[i].equals("hqaid") || headers[i].equals("選項編號")){
				im.setQaid(StringUtils.trimToNull(cells.get(i)));
			}
			if(headers[i].equals("optionAction") || headers[i].equals("選後行為")){
				for (OptionAction oa: OptionAction.values()) {
					if(oa.name().equals(cells.get(i)) || oa.getTitle().equals(cells.get(i))){
						im.setOptionAction(oa.name());
					}
				}
			}
			if(headers[i].equals("將現有知識點轉為選項")){
				try {
					im.setMergeKidAsSingleOption(new Long(cells.get(i)));
				}
				catch (Exception ignoreIt) {}
			}
			if(headers[i].equals("pipe") || headers[i].equals("QAID")){
				im.setPipe(cells.get(i));
			}
			if(headers[i].equals("question") || headers[i].equals("問題")){
				im.setQuestion(cells.get(i));
			}
			if(headers[i].equals("answer") || headers[i].equals("答案")){
				im.setAnswer(cells.get(i));
			}
			if(headers[i].equals("matchSentences") || headers[i].equals("例句")){
				im.setMatchSentences(cells.get(i));
			}
			if(headers[i].equals("script") || headers[i].equals("腳本")){
				im.setScript(cells.get(i));
			}
			if(headers[i].equals("extraParams")){
				im.setExtraParams(cells.get(i));
			}
			if(headers[i].equals("expiry") || headers[i].equals("期限")){
				im.setExpiry(cells.get(i));
			}
			if(headers[i].equals("menuView") || headers[i].equals("下層顯示模式")){
				for (MenuView msb: MenuView.values()) {
					if(msb.name().equals(cells.get(i)) || msb.getTitle().equals(cells.get(i))){
						im.setMenuView(msb.name());
					}
				}
			}
			if(headers[i].equals("inputType") || headers[i].equals("下層輸入模式")){
				for (MenuSelectionBehavior msb: MenuSelectionBehavior.values()) {
					if(msb.name().equals(cells.get(i)) || msb.getTitle().equals(cells.get(i))){
						im.setInputType(msb.name());
					}
				}
			}
		}
		for (int i = 12; i < headers.length && i < cells.size(); i++) {
			if (StringUtils.startsWithIgnoreCase(headers[i], "h_qa_") && cells.get(i) != null) {
				im.setHqaClass(StringUtils.trim(headers[i].split("h_qa_")[1]));
				try {
					im.setText(cells.get(i));
				} catch (Exception e) {
					System.out.println(e);
				}
				break;
			}
			else if (StringUtils.startsWithIgnoreCase(headers[i], "選項標題") && cells.get(i) != null) {
				im.setHqaClass(StringUtils.trim(StringUtils.substringBetween(headers[i], "(Lv", ")")));
				try {
					im.setText(cells.get(i));
				} catch (Exception e) {
					System.out.println(e);
				}
				break;
			}
		}
		return im;
	}

	/***************************************************
	 * URL: /XXXXX?f=value doGet(): get file of index "f" from List<FileMeta> as
	 * an attachment
	 ****************************************************/
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Integer opLogId = (Integer) request.getAttribute("opLogId");
		OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
        Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
		if ((AdminGroupFacade.getInstance().getFromSession(request.getSession()).getSystemAdminCURD() & AdminGroup.O3) == 0) {
			return;
		}

		AdminUser adminUser = AdminUserFacade.getInstance().getFromSession(request.getSession());

		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		// 1. Get f from URL upload?f="?"
		int fileIdx = Integer.parseInt(StringUtils.defaultString(request.getParameter("f"), "0"));

		com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(request.getSession());
		QAUtil qautil = QAUtil.getInstance(t);

		boolean dryRun = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("dryRun"), "true"));
		System.out.println("dryRun=" + dryRun);

		List<FileMeta> files = (List<FileMeta>) request.getSession().getAttribute("uploaded");
		
		ExecutorService executor = Executors.newFixedThreadPool(4);

		// 2. Get the file of index "f" from the list "files"
		FileMeta getFile = files.get(fileIdx);

		PrintWriter out = response.getWriter();
		out.println("<html><head>"
				+"<meta http-equiv='X-UA-Compatible' content='IE=edge'>"
				+"<script src='js/jquery-1.12.4.min.js'></script>"
				+"<script src='js/vendor/jquery.ui.widget.js'></script>"
				+"<script src='js/jquery.iframe-transport.js'></script>"
				+"<script src='js/lib/jquery.timeago.js'></script>"
				+"<!-- bootstrap just to have good looking page -->"
				+"<link href='" + request.getContextPath() + "/styles/bs3/bootstrap.min.css' type='text/css' rel='stylesheet'/>"
				+"<link href='" + request.getContextPath() + "/styles/bs3/bootstrap-theme.min.css' rel='stylesheet'>"
				+"<script src='" + request.getContextPath() + "/script/bs3/bootstrap.min.js'></script>"
				);
		
		request.getRequestDispatcher("header-qa.jsp").include(request, response);
		out.println("</head>");
		out.println("<body>");
		request.getRequestDispatcher("navbar-qa.jsp").include(request, response);
		out.println("<div class='table-responsive'>");

		try {

			// 3. Process the QA Data
			InputStream input = getFile.getContent();

			if (input.available() == 0) {
				out.println("File content expired, please upload it again.");
				files.remove(getFile);
			} else {

				final CellProcessor[] allProcessors = null;
				ICsvListReader listReader = null;

				try {
					String charset = com.intumit.solr.util.WiSeUtils.autoDetectCharset(input);
					input.reset();
					System.out.println("Detected charset:" + charset);

					if ("text/csv".equalsIgnoreCase(getFile.getFileType())) {
						listReader = new CsvListReader(new InputStreamReader(input, charset), CsvPreference.EXCEL_PREFERENCE);
					}
					else if ("application/vnd.ms-excel".equalsIgnoreCase(getFile.getFileType())
							|| "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equalsIgnoreCase(getFile.getFileType())
							) {
						listReader = new CsvListReader(new InputStreamReader(input, charset), CsvPreference.EXCEL_PREFERENCE);
					}
					else {
						listReader = new CsvListReader(new InputStreamReader(input, charset), CsvPreference.TAB_PREFERENCE);
					}
					

					String[] headers = null;
					// with CsvListReader)
					String dataType = WiSeUtils.cn2tw(StringUtils.substringBeforeLast(getFile.getFileName(), "."));
					// 匯入文件檔名繁簡皆判斷
					if (StringUtils.startsWithIgnoreCase(dataType, QA_DATA_FILE_NAME)) {
						headers = getQaDataHeaders(listReader);
						dataType = QAUtil.DATATYPE_COMMON_SENSE;
					}
					else if (StringUtils.startsWithIgnoreCase(dataType, CASUAL_DATA_FILE_NAME)) {
						headers = getQaDataHeaders(listReader);
						dataType = QAUtil.DATATYPE_CASUAL;
					}
					else {
						headers = listReader.getHeader(true);
					}
					System.out.println("Got Headers:==>" + Arrays.asList(headers) + "<==");
					
					/**
					 * 特定檔名攔截處理
					 */
					if (StringUtils.startsWithIgnoreCase(dataType, HIERARCHICAL_QA_DATA_FILE_NAME)) {
						// "智能客服階層式問答匯入檔" 處理完直接 return
						processHierarchicalQaExcel(t, locale, fileIdx, dryRun, listReader, headers, out, adminUser, getFile, log);
						return;
					}
					else if (StringUtils.startsWithIgnoreCase(dataType, QA_DATA_MULTI_CHANNEL_ANSWER_FILE_NAME)) {
						// "智能客服問答知識庫答案匯入檔" 處理完直接 return
						processMultiChannelAnswer(t, dryRun, listReader, headers,out);
						return;
					}
					else if (StringUtils.startsWithIgnoreCase(dataType, QA_DATA_ROBOT_CHANNEL_ANSWER_FILE_NAME)) {
						processRobotChannelAnswer(t, dryRun, listReader, headers, out);
						return;
					}
					else if (StringUtils.startsWithIgnoreCase(dataType, TEXT_CRM_SERVICE_LOG_DATA_FILE_NAME)) {
						// "智能客服問答知識庫答案匯入檔" 處理完直接 return
						processTextCrmServiceLog(t, listReader, headers, out);
						return;
					}
					else if (StringUtils.startsWithIgnoreCase(dataType, QA_EVALUATION_FILE_NAME)) {
						// "智能客服測試紀錄匯入檔" 處理完直接 return
						processEvaluationLog(t, listReader, headers, out, getFile, charset);
						return;
					}

					SolrServer server = QAUtil.DATATYPE_CASUAL.equals(dataType) ? QAUtil.getCasualServer(t, true) : t.getCoreServer4Write();
					boolean customData = false;
					List<String> cells = null;

					if (!dataType.equals(QAUtil.DATATYPE_COMMON_SENSE) && !dataType.equals(QAUtil.DATATYPE_CASUAL)) {
						customData = true;
						if (!dryRun) {
							server.deleteByQuery("(dataType_s:" + dataType + ")");
							server.commit(true, true, false);
							CustomDataDictionary.clearCache(t.getId());
							CustomDataDictionary.clear(t.getId());
							
							try {
								// wait for new searcher
								Thread.sleep(1000);
							} catch (InterruptedException ignore) {
							} 
						}
						else {
							out.println(MessageUtil.getMessage(locale, "data.type")+"：" + dataType + "<BR>");
							out.println("<a href='processData?f=" + fileIdx + "&dryRun=false&action=import&filename="+getFile.getFileName()+"' class='btn btn-danger'>" + MessageUtil.getMessage(locale, "global.import") + "</a>");
							out.println("&nbsp;<a href='fileUpload.jsp' class='btn btn-default'>" + MessageUtil.getMessage(locale, "global.cancel") + "</a>");
						}
					}

					out.println("<div class='row'>");
					out.println("<div class='col-md-12'>Each column shows Status, "+MessageUtil.getMessage(locale, "number.rows")+" / "+MessageUtil.getMessage(locale, "line.number")+" / " + MessageUtil.getMessage(locale, "column.number") + " and " + MessageUtil.getMessage(locale, "global.content") + "</div>");
					out.println("</div>");
					out.println("<table class='table table-striped table-bordered'>");

					List<String> boostTerms = WhiteWikiWordFacade.getInstance().whiteList();
					String robotName = RobotFormalAnswers.giveMeRandomAnswer(t.getId(), "ROBOT_NAME", MessageUtil.getMessage(locale, "robot.name"));
					int counter = 0;
					
					try {
						nextRow: while ((cells = listReader.read()) != null && (customData || ++counter < t.getMaxBatchUploadSize())) {
							System.out.println("ProcessQADataServlet import counter: " + counter);
							
							boolean gotId = false;
							QA doc = null;
							cells = truncateTailNulls(cells);
							String id = null;
							Long kid = null;
							String[] kps = null;

							for (int i=0; i < headers.length && i < cells.size(); i++) {
								if (headers[i] == null || headers[i].endsWith("(ig)"))
									continue;

								if (StringUtils.equalsIgnoreCase(headers[i], "id")) {
									String fnVal = cells.get(i);

									if (StringUtils.trimToNull(fnVal) != null)
										gotId = true;

									if (!customData) {
										fnVal = StringUtils.trim(fnVal).replaceAll("\\s", "");
										kid = new Long(fnVal);
									}

									fnVal = dataType + "-" + fnVal;
									id = fnVal;

									SolrDocument mainDoc = null;

									if (kid != null) {
										if (dataType.equals(QAUtil.DATATYPE_COMMON_SENSE)) {
											mainDoc = qautil.getMainQASolrDocument(kid, true);
										}
										else if (dataType.equals(QAUtil.DATATYPE_CASUAL)) {
											mainDoc = qautil.getCasualQASolrDocument(t, kid);
										}
									}

									if (mainDoc != null) {
										doc = new QA(mainDoc);
									}
									else {
										doc = new QA();
									}
								}
								else if (StringUtils.equalsIgnoreCase(headers[i], "KNOWLEDGE_POINT")) {
									if (StringUtils.trimToNull(cells.get(i)) != null) {
										kps = StringUtils.split(cells.get(i), "[,\\|]");

									}
								}

							}

							if (customData && doc == null) {
								doc = new QA();
							}
							List<String> moreAltsFromMultilineQuestion = new ArrayList<String>();
							List<String> batchNlpMultilineQuestions = new ArrayList<String>();
							for (int i=0; i < headers.length && i < cells.size(); i++) {
								if (headers[i] == null || headers[i].endsWith("(ig)"))
									continue;
								
								if ("DELETE_MARK_ig".equalsIgnoreCase(headers[i])) {
									if (cells.size() > i && ("Y".equalsIgnoreCase(cells.get(i)) || "true".equalsIgnoreCase(cells.get(i)))) {
										System.out.println("Delete mark = true, skip this row [" + cells + "]");
										continue nextRow;
									}
								}

								boolean isMultivalue = isMultiValue(headers[i]);
								boolean guessFieldType = needGuessFieldType(headers[i]);
								String fieldNamePrefix = getFieldNamePrefix(headers[i]);
								boolean isRichText = (QAChannel.get(t.getId(), "web").getType() == QAChannelType.RICH_TEXT);
								Pattern p = Pattern.compile("<(.+?)>");
							    
								if ("HIERARCHICAL_QA".equalsIgnoreCase(fieldNamePrefix) && cells.size() > i) {
									String text = cells.get(i);
									if(StringUtils.isNotBlank(text)){
										try{
											JSONObject data = null;
											try {
												// 往回相容，現在 HierarchicalQA.HIERARCHICAL_QA_FIELD_NAME 應該要存的是 JSONObject，不是 JSONArray
												// 如果發現 Excel 還是 JSONArray... 就靠 Exception 後面還是把他 handle 掉
												data = new JSONObject(text);
											}
											catch (Exception backwardCompatible) {
												JSONArray children = new JSONArray(text);
												data = new JSONObject();
												data.put("children", children);
											}
											if (data.length() > 0) {
												doc.setField(HierarchicalQA.HIERARCHICAL_QA_FIELD_NAME, data.toString());
												doc.setField("ANSWER_PLUGIN_ID_s", QAPlugins.HQA_ID);
											}
										}catch(Exception e){
											e.printStackTrace();
										}
									}
									continue;
								}

								String[] toFNs = guessFieldType ? getFieldName(fieldNamePrefix, isMultivalue) : new String[] {headers[i]};

								if (toFNs == null || cells.size() <= i)
									continue;

								String fnVal = cells.get(i);

								if ("QUESTION".equalsIgnoreCase(fieldNamePrefix)) {
									for (String boostTerm: boostTerms) {
										if (StringUtils.contains(fnVal, boostTerm)) {
											doc.addField("BoostTerm_mt", boostTerm);
										}
									}
									if (StringUtils.trimToNull(fnVal) != null) {
										fnVal = TemplateUtil.processByTagName(fnVal, "ROBOT_NAME", new TemplateUtil.Replace("你"));

										List<String> multivalue = new ArrayList<String>(Arrays.asList(fnVal.split("\n")));
										if (multivalue.size() > 1) {
											fnVal = multivalue.get(0);
											moreAltsFromMultilineQuestion = multivalue;
											//System.out.println("Got moreAlts from multilines quesiton:" + moreAltsFromMultilineQuestion);
										}
									}
									else {
										fnVal = "";
									}
								}
								else if ("ALT_TEMPLATE".equalsIgnoreCase(fieldNamePrefix)) {
									if (StringUtils.trimToNull(fnVal) != null) {
										String[] applyIds = fnVal.split("[\\n\\r]+");
										for (String applyIdStr: applyIds) {
											String applyId = StringUtils.substringBefore(applyIdStr, ":");
											String[] kws = StringUtils.split(StringUtils.substringAfter(applyIdStr, ":"), ",");
											
											QAAltTemplate tpl = QAAltTemplate.getByKey(t.getId(), applyId);

											if (tpl != null) {
												if (kws != null && kws.length > 0) {
													for (int k=1; k <= kws.length; k++) {
														String tplStr = tpl.getTemplate();
														if (k==1) tplStr = tplStr.replaceAll("\\{\\{KEYWORD\\}\\}", kws[k-1]);
														tplStr = tplStr.replaceAll("\\{\\{KEYWORD" + k + "\\}\\}", kws[k-1]);
														moreAltsFromMultilineQuestion.addAll(Arrays.asList(tplStr.split("[\\n\\r]+")));
													}
												}
												else {
													String tplStr = tpl.getTemplate();
													if (tplStr != null) {
														moreAltsFromMultilineQuestion.addAll(Arrays.asList(tplStr.split("[\\n\\r]+")));
													}
												}
											}
										}
									}
								}
								else if ("ANSWER".equalsIgnoreCase(fieldNamePrefix)) {
									if (StringUtils.trimToNull(fnVal) != null) {
										List<String> multivalue = new ArrayList<String>(Arrays.asList(StringUtils.splitByWholeSeparator(fnVal, "///")));
										if (multivalue.size() > 1) {
											fnVal = multivalue.remove(0);
											doc.setField("ANSWER_ALT_ms", multivalue);
										}
										if (StringUtils.trimToNull(fnVal) != null) {
											Matcher matcher = p.matcher(fnVal);
											// if we find a match, get the group
											StringBuilder strbu = new StringBuilder();
											int pos = 0;
											while(matcher.find()) {
												strbu.append(fnVal.substring(pos, matcher.start()).replace(" ", "&nbsp;"));
												strbu.append(fnVal.substring(matcher.start(), matcher.end()));
												pos = matcher.end();
											}
											if(strbu.length() > 0)
											{
												strbu.append(fnVal.substring(pos));
												fnVal = strbu.toString();
											}
											else
												fnVal = fnVal.replace(" ", "&nbsp;");
											if (isRichText)
											{   
												int ulIndex = fnVal.indexOf("<ul>");
												if (ulIndex > 0)
												{
                                                    int tblIndex = fnVal.indexOf("<table>");
												    if(tblIndex > 0)
												    {
                                                        String outsideTable = fnVal.substring(0, tblIndex);
                                                        outsideTable = WiSeUtils.nl2br(outsideTable);
                                                        fnVal = outsideTable + fnVal.substring(tblIndex);
    												    
                                                        tblIndex = fnVal.indexOf("</table>");
                                                        outsideTable = fnVal.substring(tblIndex);
                                                        outsideTable = WiSeUtils.nl2br(outsideTable);
                                                        fnVal = fnVal.substring(0, tblIndex) + outsideTable;
												    }    
													String beforeUl = fnVal.substring(0, ulIndex);
													beforeUl = WiSeUtils.nl2br(beforeUl).replace("❤","&hearts;");
													fnVal = beforeUl + fnVal.substring(ulIndex);            
												}
												else
													fnVal = WiSeUtils.nl2br(fnVal).replace("❤","&hearts;");
												cells.set(i, fnVal);
											}
											else
											{
												cells.set(i, WiSeUtils.nl2br(cells.get(i)).replace("❤","&hearts;"));
											}
											MultiChannelAnswer.save(t.getId(), id, "web", "unknown", fnVal);
										}
									}
								}
								else if (StringUtils.equalsIgnoreCase(headers[i], "id")) {
									fnVal = id;
								}
								else if (StringUtils.startsWithIgnoreCase(headers[i], "answer_") && headers[i].split("_").length == 3) {
									String[] header = headers[i].split("_");
									MultiChannelAnswer.save(t.getId(), id, header[1], header[2], cells.get(i));
								}
								else if (StringUtils.startsWithIgnoreCase(headers[i], "answer_") && headers[i].split("_").length == 4) {
									String[] header = headers[i].split("_");
									MultiChannelAnswer.save(t.getId(), id, header[1], header[2], null, cells.get(i));
								}

								for (String fn: toFNs) {
									if (StringUtils.trimToNull(fnVal) != null) {
										if (isMultivalue) {
											List<String> multivalue = new ArrayList<String>(Arrays.asList(fnVal.split("\n")));

											if ("QUESTION_ALT".equalsIgnoreCase(fieldNamePrefix)) {
												if (moreAltsFromMultilineQuestion != null) {
													multivalue.addAll(moreAltsFromMultilineQuestion);
												}

												doc.setField("QUESTION_ALT_TPL_ms", multivalue);
											}
											else if ("INHERITANT_ALT_TEMPLATE".equalsIgnoreCase(fieldNamePrefix)) {
												if (StringUtils.trimToNull(fnVal) != null) {
													String[] ihAltTemplates = fnVal.split("[\\n\\r]+");
													List<String> ihAltTemplateList = Arrays.asList(ihAltTemplates);
													List<String> mkeys = new ArrayList<String>();
													for (String ihAltTpl: ihAltTemplates) {
														mkeys.add(StringUtils.substringBefore(ihAltTpl, ":"));
													}
													doc.setField(QA.FN_INHERITANT_ALT_TEMPLATE_MKEYS, mkeys);
													doc.setField(QA.FN_INHERITANT_ALT_TEMPLATES, ihAltTemplateList);
												}
											}
											else {
												doc.setField(fn, multivalue);
											}
										}
										else {
											if ("IsNotForMlt".equalsIgnoreCase(fieldNamePrefix)) {
												if (StringUtils.trimToNull(fnVal) != null) {
													doc.setField(fn, Boolean.valueOf((String)fnVal));
												}
												else {
													doc.setField(fn, Boolean.FALSE);
												}
											}
											else if ("IsNotForSearch".equalsIgnoreCase(fieldNamePrefix)) {
												if (StringUtils.trimToNull(fnVal) != null) {
													doc.setField(fn, Boolean.valueOf((String)fnVal));
												}
												else {
													doc.setField(fn, Boolean.FALSE);
												}
											}
											else if ("IsNotAppendMlt".equalsIgnoreCase(fieldNamePrefix)) {
												if (StringUtils.trimToNull(fnVal) != null) {
													doc.setField(fn, Boolean.valueOf((String)fnVal));
												}
												else {
													doc.setField(fn, Boolean.FALSE);
												}
											}
											else if ("IsBanned".equalsIgnoreCase(fieldNamePrefix)) {
												if (StringUtils.trimToNull(fnVal) != null) {
													doc.setField(fn, Boolean.valueOf((String)fnVal));
												}
												else {
													doc.setField(fn, Boolean.FALSE);
												}
											}
											else if ("TestCases".equalsIgnoreCase(fieldNamePrefix)) {
												if (StringUtils.trimToNull(fnVal) != null) {
													batchNlpMultilineQuestions.addAll(new ArrayList<String>(Arrays.asList(fnVal.split("\n"))));
												}
											}
											else if ("REGEXP".equalsIgnoreCase(fieldNamePrefix)) {
												// 對生活問答才有用，代表各種問法是「正規表示法」
												if (StringUtils.trimToNull(fnVal) != null) {
													doc.setField(fn, Boolean.valueOf((String)fnVal));
												}
												else {
													doc.setField(fn, Boolean.FALSE);
												}
											}
											else if (fn.endsWith("_dt")) {
												try {
													SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
													Date d = sdf.parse((String)fnVal);
													doc.setField(fn, d);
												} catch (Exception e) {
													e.printStackTrace();
												}
											}
											else {
												if (StringUtils.trimToNull(fnVal) != null) {
													doc.setField(fn, fnVal);
												}
											}
										}
									}
									else {
										if ("QUESTION_ALT".equalsIgnoreCase(fieldNamePrefix) && moreAltsFromMultilineQuestion != null) {
											doc.setField("QUESTION_ALT_TPL_ms", moreAltsFromMultilineQuestion);
										}
									}
								}
							}
							
							if (batchNlpMultilineQuestions.size() > 0) {
								List<String> dupCheck = new ArrayList<String>();
								List<String> currentAlts = doc.getQuestionAltTemplates();
								if (currentAlts == null) currentAlts = new ArrayList<String>();
								
								for (String altTplLine: currentAlts) {
									dupCheck.add(QA.parseAndGetQAAltOnly(altTplLine));
								}
								boolean dirty = false;
								
								for (String s: batchNlpMultilineQuestions) {
									// 空例句跳過
									if(StringUtils.isBlank(s)) continue;
									String cleanedQ = QAUtil.removeSymbols(s, t.getLocale());
									String altTpl = QAUtil.nlp(t, qautil, cleanedQ, locale, ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, TemperalAndNumberStrategy.PREBUILD_STRATEGY_DONT_CHANGE_ANYTHING).getSegResult();
									
									if (!dupCheck.contains(altTpl)) {
										dirty = true;
										dupCheck.add(altTpl); 
										
										JSONObject obj = new JSONObject();
										obj.put("alt.editor", adminUser.getId());
										obj.put("testCase", s);
										obj.put("testCase.editor", adminUser.getId());
										
										String altTplLine = altTpl + " // " + obj.toString();
										currentAlts.add(altTplLine);
									}
								}
								
								if (dirty)
									doc.setField("QUESTION_ALT_TPL_ms", currentAlts);
							}

							String status = "Unknown";
							if (gotId) {
								Date now = new Date();
								doc.setUpdateInfo(now, adminUser);
								doc.setCreateInfo(now, adminUser);
								doc.setDataType(dataType);

								if (!dryRun) {
									try {
										// 先設定 kid 再存入索引，不然索引內會有沒 kid 的資料
										if (dataType.equals(QAUtil.DATATYPE_COMMON_SENSE) || dataType.equals(QAUtil.DATATYPE_CASUAL)) {
											doc.setKid(kid);
										}
										
										if (dataType.equals(QAUtil.DATATYPE_COMMON_SENSE)) {
											server.add(doc);
											server.commit(true, true, true);
											QAAltBuildQueue.add(t.getId(), id, kid, doc.getQuestionAltTemplates(), adminUser.getLoginName());
											status = "OK (" + kid + ")";
											if(batchNlpMultilineQuestions.size() > 0) {
												Set<String> dedup = new HashSet<String>();
												for(String testCase : batchNlpMultilineQuestions) {
													if(!dedup.contains(testCase)) {
														EvaluationLogEntity.createEvaluationLogAndSave(request, 2, kid, testCase, (String)doc.getFieldValue(QA.FN_ANSWER), doc.getQaCategory());	
														dedup.add(testCase);
													}
												}
											}
										}
										else if (dataType.equals(QAUtil.DATATYPE_CASUAL)) {
											final CasualQASaver qaSaver = new CasualQASaver(doc, t);
										    Runnable worker = new Runnable() {
												@Override
												public void run() {
													qaSaver.save();
												}
										    };
										    executor.execute(worker);
											status = "OK (" + kid + ")";
										}
										else {
											server.add(doc);
											server.commit(true, true, true);
											status = "OK (" + id + ")";
										}
									log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
									}catch (Exception e) {
										log.setStatusMessage(OperationLogEntity.Status.FAILED);
										log.appendToMoreDetails(e.toString());
									}
								}
								else {
									status = "dry run";
								}
							}
							else {
								status = MessageUtil.getMessage(locale, "global.without")+MessageUtil.getMessage(locale, "knowledge.num");
							}
							
							out.println("<tr>");
							out.println(
									String.format(
											"<td class='col-md-2'>%s</td><td class='col-md-1'>%s / %s / %s</td>%s",
											status,
											listReader.getLineNumber(),
											listReader.getRowNumber(),
											cells.size(), cellsToTD(cells)));
							out.println("</tr>");
						}
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
					out.println("</table>");

					if (!dryRun) {
						try {
							// wait for softCommit
							Thread.sleep(1000);
						} catch (InterruptedException ignore) {
						} 
						
						if (customData) {
							CustomDataDictionary.clear(t.getId());
						}
						
						log.update();
					}

					out.println("<HR><A HREF='fileUpload.jsp' class='btn btn-default'>RETURN</a>");
					
					if (!dryRun && !dataType.equals(QAUtil.DATATYPE_COMMON_SENSE) && !dataType.equals(QAUtil.DATATYPE_CASUAL)) {
						if (t.getEnableScenario() && (AdminGroupFacade.getInstance().getFromSession(request.getSession()).getSystemAdminCURD() & AdminGroup.O3) > 0) {
							out.println("&nbsp;&nbsp;&nbsp;&nbsp;<A HREF='qaScenarioWizard.jsp?step=1&dataType=" + dataType + "' class='btn btn-success'>Train Entities</a>");
						}
					}

				} finally {
					if (listReader != null) {
						listReader.close();
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		out.println("</div>");
	    // Wait until all threads are finish
	    try {
		    executor.shutdown();
			executor.awaitTermination(24, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		out.println("</body></html>");
	}

	private void processTextCrmServiceLog(Tenant t, ICsvListReader listReader, String[] headers, PrintWriter out) {
		try {
			List<String> cells = null;
			
			String qaId = null;
			JSONArray conversations = null;//new JSONObject();
			int rowCnt = 0;
			
			while ((cells = listReader.read()) != null) {
				rowCnt++;
				String tmpId = null;
				for (int i=0; i < headers.length && i < cells.size(); i++) {
					if (StringUtils.equalsIgnoreCase(headers[i], "qaId")) {
						tmpId = cells.get(i);
					}
				}
				
				if (StringUtils.trimToNull(tmpId) == null) {
					System.out.println("Row [" + rowCnt + "] has no qaId ");
					continue;
				}
				
				if (!StringUtils.equalsIgnoreCase(qaId, tmpId)) {
					if (qaId != null) {
						out.println(saveTextCrmConversations(t, qaId, conversations));
						out.println("<br>");
					}
					
					qaId = tmpId;
					conversations = new JSONArray();
				}
				
				JSONObject msg = new JSONObject();
				for (int i=0; i < headers.length && i < cells.size(); i++) {
					String fn = headers[i];
					String val = cells.get(i);
					
					if (StringUtils.trimToNull(fn) == null)
						continue;
					
					try {
						msg.put(fn, val);
					}
					catch (JSONException e) {
						e.printStackTrace();
					}
				}
				conversations.add(msg);
			}

			if (StringUtils.trimToNull(qaId) != null) {
				out.println(saveTextCrmConversations(t, qaId, conversations));
				out.println("<br>");
			}
			out.println("DONE!!!");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void processEvaluationLog(Tenant t, ICsvListReader listReader, String[] headers, PrintWriter out, FileMeta getFile, String charset) {
		try {
			out.println("<table class='table table-striped table-bordered'>");
			if ("text/csv".equalsIgnoreCase(getFile.getFileType()) || "text/plain".equalsIgnoreCase(getFile.getFileType())) {
				Date now = new Date();
				
				Blob blob = Hibernate.createBlob(getFile.getContent());
			    
			    QAEvaluationLogQueue.add(t.getId(), blob, t.getId() + "_" + System.currentTimeMillis() + "_" + getFile.getFileName(), now, charset);

				out.println("<script>window.location.replace('qaEvaImportList.jsp');</script>");
			} else {
				out.println("請上傳副檔名為text/csv!!!");
			}
			out.println("</table>");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 認以下欄位
	 * tag	
	 * qaId	
	 * timestamp
	 * identity	
	 * message
	 * 
	 * @param t
	 * @param qaId
	 * @param conversations
	 */
	private TextCrmServiceLogEntity saveTextCrmConversations(Tenant t, String qaId, JSONArray conversations) {
		TextCrmServiceLogEntity e = new TextCrmServiceLogEntity();
		int fromUser = 0;
		int fromBot = 0;
		String firstUserMsg = null;
		String lastUserMsg = null;
		String lastUserIdentity = null;
		Date createdAt = null;
		Date lastMsgTs = null;
		String lastTag = null;
		String lastIp = null;
		
		try {
			for (int i=0; i < conversations.length(); i++) {
				JSONObject m = conversations.getJSONObject(i);
				String identity = m.getString("identity");
				String msg = m.optString("message");
				if (msg == null)
					continue;
				
				String tsStr = m.optString("timestamp");
				lastTag = m.optString("tag");
				
				if (TextCrmServiceLogEntity.isBotName(identity)) {
					fromBot++;
				}
				else {
					fromUser++;
					if (firstUserMsg == null) firstUserMsg = msg;
					lastUserMsg = msg;
					lastUserIdentity = identity;
					lastIp = m.optString("ipAddress");
				}

				SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:SS");
				try {
					lastMsgTs = sdf.parse(tsStr);
				}
				catch (ParseException e1) {
					e1.printStackTrace();
				}
				
				if (i == 0) {
					createdAt = lastMsgTs;
				}
			}
			
			e.setConversations(conversations.toString(2));
			e.setFirstMessage(firstUserMsg);
			e.setLastMessage(lastUserMsg);
			e.setQaId(qaId);
			e.setIdentity(lastUserIdentity);
			e.setClientIp(StringUtils.defaultString(lastIp, "127.0.0.1"));
			e.setTenantId(t.getId());
			e.setStatMsgCountFromUser(fromUser);
			e.setStatMsgCountFromRobot(fromBot);
			e.setStatMsgCountTotal(conversations.length());
			e.setTag(lastTag);
			e.setTsCreated(createdAt);
			e.setTimestamp(lastMsgTs);
			
			TextCrmServiceLogEntity.save(e);
		}
		catch (JSONException e1) {
			e1.printStackTrace();
		}
		
		return e;
	}

	/**
	 * 
	 * @param t
	 * @param dryRun @NOT_IMPLEMENT
	 * @param listReader
	 * @param headers
	 * @param out
	 */
	private void processMultiChannelAnswer(Tenant t, boolean dryRun, ICsvListReader listReader, String[] headers, PrintWriter out) {
		try {
			List<String> cells = null;
			
			nextRow: while ((cells = listReader.read()) != null) {
				String id = null;
				for (int i=0; i < headers.length && i < cells.size(); i++) {
					if (StringUtils.equalsIgnoreCase(headers[i], "id")) {
						id = cells.get(i);
					}
				}
				
				for (int i=0; i < headers.length && i < cells.size(); i++) {
					if (StringUtils.startsWithIgnoreCase(headers[i], "answer_") && headers[i].split("_").length == 3) {
						String[] header = headers[i].split("_");
						MultiChannelAnswer.save(t.getId(), QAUtil.DATATYPE_COMMON_SENSE+"-"+id, header[1], header[2], cells.get(i));
					}
				}
			}
			out.println("DONE!!!");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param t
	 * @param dryRun @NOT_IMPLEMENT
	 * @param listReader
	 * @param headers
	 * @param out
	 */
	private void processRobotChannelAnswer(Tenant t, boolean dryRun, ICsvListReader listReader, String[] headers, PrintWriter out) {
		try {
			List<String> cells = null;
			
			nextRow: while ((cells = listReader.read()) != null) {
				String id = null;
				String channel = null;
				String userType = null;
				String answer = null;
				String answerMood = null;
				String answerType = null;
				String answerVoice = null;
				String extraParamsStr = null;
				for (int i = 0; i < headers.length && i < cells.size(); i++) {
					if (StringUtils.equalsIgnoreCase(headers[i], "id")) {
						id = cells.get(i);
					}
				}

				for (int i = 0; i < headers.length && i < cells.size(); i++) {
					if (StringUtils.startsWithIgnoreCase(headers[i], "answer_")
							&& headers[i].split("_").length == 3) {
						String[] header = headers[i].split("_");
						channel = header[1];
						userType = header[2];
						answer = cells.get(i);
					}
					if (StringUtils.equalsIgnoreCase(headers[i], "answerMood")) {
						if(StringUtils.equalsIgnoreCase(cells.get(i), MultiChannelAnswer.ROBOT_MOOD_CURIOUS) || StringUtils.equalsIgnoreCase(cells.get(i), MultiChannelAnswer.ROBOT_MOOD_JOY) || StringUtils.equalsIgnoreCase(cells.get(i), MultiChannelAnswer.ROBOT_MOODR_HAPPY)){
							answerMood = cells.get(i);
						} else {
							out.println("Id: " + id + " : answerMood wrong!!!<br>");
							break;
						}
					}
					if (StringUtils.equalsIgnoreCase(headers[i], "answerType")) {
						if(StringUtils.equalsIgnoreCase(cells.get(i), MultiChannelAnswer.ROBOT_ANSWER_GENERAL) || StringUtils.equalsIgnoreCase(cells.get(i), MultiChannelAnswer.ROBOT_ANSWER_ADVANCE)){
							answerType = cells.get(i);
						} else {
							out.println("Id: " + id + " : answerType wrong!!!<br>");
							break;
						}
					}
					if (StringUtils.equalsIgnoreCase(headers[i], "answerVoice")) {
						answerVoice = cells.get(i);
					}
					if (StringUtils.equalsIgnoreCase(headers[i], "extraParam")) {
						try {
							new JSONObject(cells.get(i) == null ? "{}" : cells.get(i));
							extraParamsStr = cells.get(i);
						} catch (Exception e) {
							out.println("Id: " + id + ", json format error!!!<br>");
						}
					}
				}
				try {
					if ( answerType.equals(MultiChannelAnswer.ROBOT_ANSWER_ADVANCE) ) {
						MultiChannelAnswer.save(t.getId(), QAUtil.DATATYPE_COMMON_SENSE+"-"+id, channel, userType, answer,
								MultiChannelAnswer.ROBOT_ANSWER_ADVANCE, answerVoice, answerMood);
						if ( extraParamsStr != null ) {
							MultiChannelAnswer mca = MultiChannelAnswer.get(t.getId(), QAUtil.DATATYPE_COMMON_SENSE+"-"+id, channel, userType);
							if ( mca != null ) {
								mca.setExtraParameters(extraParamsStr);
								MultiChannelAnswer.saveOrUpdate(mca);
							}
						}
					} else {
						MultiChannelAnswer.save(t.getId(), QAUtil.DATATYPE_COMMON_SENSE+"-"+id, channel, userType, answer,
								MultiChannelAnswer.ROBOT_ANSWER_GENERAL, null, null);
					}
				} catch (Exception ex) {}
			}
			out.println("DONE!!!");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param t
	 * @param dryRun
	 * @param listReader
	 * @param headers
	 * @param out
	 * @param user
	 */
	private void processHierarchicalQaExcel(Tenant t, Locale locale, int fileIdx, boolean dryRun, ICsvListReader listReader, String[] headers, PrintWriter out, AdminUser user, FileMeta getFile, OperationLogEntity log) {
		int rowCount = 0;
		List<String> cells = null;
		
		try {
			String dataType = QAUtil.DATATYPE_COMMON_SENSE;
			QAUtil qautil = QAUtil.getInstance(t);
			SolrServer server = t.getCoreServer4Write();
			JSONObject rootJson = new JSONObject();
			rootJson.put("children", new JSONArray());
			
			String currKid = null;
			String currEntryPoint = null;
			List<ImportHierarchicalQA> rowsOfCurrQ = new ArrayList<>();
			int updatedCount = 0;
			
			out.println("<h1>Preview data</h1>");
			out.println("<table class='table table-striped table-bordered'>");
			out.println("<tr><td></td>" + cellsToTD(Arrays.asList(headers), true) + "</tr>");
			
			while (true) {
				rowCount++;
				cells = listReader.read();
				if (cells == null) cells = Arrays.asList(new String[] {"", "", "", ""}); // 省麻煩，弄四欄空白符合後續比對邏輯
				cells = trimCellsToEmpty(cells);
				int hqaIdIdx = Arrays.asList(headers).indexOf("選項編號");
				String hid = StringUtils.trimToNull(hqaIdIdx != -1 
						? cells.get(hqaIdIdx) 
						: cells.size() > 3 
							? cells.get(3) 
							: null);
				boolean isEOF = hid == null || "0".equals(hid); // 選項編號如果是空白或者零代表已經跑到整個 csv 結尾
				String mark = "<h2 class='text text-success glyphicon glyphicon-ok'></h2>";
				
				if (isEOF) {
					cells = Arrays.asList(new String[] {"", "", "", ""}); // 省麻煩，弄四欄空白符合後續比對邏輯
				}
				
				int entryPointIdx = Arrays.asList(headers).indexOf("進入點");
				ImportHierarchicalQA im = getHQa(cells, headers);
				String nextId = StringUtils.trimToNull(cells.get(0));
				
				// 第一個需要額外處理
				if (currKid == null && nextId != null) {
					currKid = nextId;
					
					if (entryPointIdx != -1) {
						String tmp = StringUtils.trimToNull(cells.get(entryPointIdx));
						if (tmp != null) {
							currEntryPoint = tmp;
						}
					}
					else {
						currEntryPoint = null;
					}
					nextId = null;
				}
				
				// 確定該題目是否存在
				SolrDocument mainDoc = qautil.getMainQASolrDocument(Long.valueOf(currKid), true);
				if (mainDoc == null) {
					mark = "<h2 class='text text-danger glyphicon glyphicon-remove'></h2>";
				}
				else if (im.getMergeKidAsSingleOption() != null) {
					mark = "<h2 class='text text-danger glyphicon glyphicon-warning-sign'></h2>";
				}
				
				if (isEOF || nextId != null) {
					// 要換新 ID，把現在的 ID 處理完再切換
					int prevClass = 1;
					LinkedList<JSONObject> parentStack = new LinkedList<JSONObject>();
					List<QA> toBeMergedQAs = new ArrayList<>();
					JSONObject parent = rootJson;
					QA currQA = null;
					
					if (mainDoc != null) {
						currQA = new QA(mainDoc);
					}
					
					for (ImportHierarchicalQA imHqa: rowsOfCurrQ) {
						if (StringUtils.isBlank(imHqa.getHqaClass()))
							break;
						
						JSONObject jo = getHQaObject(imHqa);
						
						if (imHqa.getMergeKidAsSingleOption() != null && currQA != null) {
							SolrDocument toBeMergedDoc = qautil.getMainQASolrDocument(imHqa.getMergeKidAsSingleOption());
							
							if (toBeMergedDoc != null) {
								QA toBeMergedQA = new QA(toBeMergedDoc);
								qautil.mergeQA(currQA, toBeMergedQA, true, user);
								MultiChannelAnswer mca = MultiChannelAnswer.get(t.getId(), toBeMergedQA.getId(), QAChannel.DEFAULT_CHANNEL_CODE, QAUserType.DEFAULT_USERTYPE_CODE);
								jo.put("answer", mca != null ? mca.getAnswer() : "");
								toBeMergedQAs.add(toBeMergedQA);
							}
						}
						
						int currClass = Integer.valueOf(imHqa.getHqaClass());
						
						if (!parent.has("children")) {
							parent.put("children", new JSONArray());
						}
						
						if (currClass == prevClass) {
							parent.getJSONArray("children").add(jo);
						} 
						else if (prevClass < currClass) {
							// 出現下層
							JSONArray children = parent.getJSONArray("children");
							JSONObject newParent = children.getJSONObject(children.length() - 1); // 最後一個抓出來當作新 parent
							if (!newParent.has("children")) {
								newParent.put("children", new JSONArray());
							}
							newParent.getJSONArray("children").add(jo);
							parentStack.add(parent);
							parent = newParent;
						}
						else if (prevClass > currClass) {
							// 回到上N層
							for (int i=prevClass; i > currClass + 1; i--) {
								parentStack.removeLast();
							}
							parent = parentStack.removeLast();
							parent.getJSONArray("children").add(jo);
						}
						
						prevClass = currClass;
					}
					
					// 準備寫入索引
					if (currEntryPoint != null) {
						rootJson.put("entryPoint", currEntryPoint);
					}

					
					if (dryRun) {
						
					}
					else if (currQA != null) {
						try {
							// 被合併的 QA 會被 disabled，所以需要存一下
							for (QA toBeMergedQA: toBeMergedQAs) {
								server.add(toBeMergedQA);
							}
							toBeMergedQAs = new ArrayList<>();
							
							currQA.setField(HierarchicalQA.HIERARCHICAL_QA_FIELD_NAME, rootJson.toString());
							currQA.setField("ANSWER_PLUGIN_ID_s", QAPlugins.HQA_ID);
							currQA.setKid(Long.valueOf(currKid));
							server.add(currQA);
							server.commit(true, true, true);
							updatedCount++;
							QAAltBuildQueue.add(t.getId(), dataType + "-" + currKid, Long.valueOf(currKid),
									currQA.getQuestionAltTemplates(), user.getLoginName());
							log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
						}catch (Exception e) {
							log.setStatusMessage(OperationLogEntity.Status.FAILED);
							log.appendToMoreDetails(e.toString());
						}
					}
					
					// Reset 
					rootJson = new JSONObject();
					currKid = nextId;
					if (entryPointIdx != -1) {
						String tmp = StringUtils.trimToNull(cells.get(entryPointIdx));
						if (tmp != null) {
							currEntryPoint = tmp;
						}
					}
					else {
						currEntryPoint = null;
					}
					rowsOfCurrQ = new ArrayList<>();
					if (!isEOF && im != null) {
						out.println("<tr><td>" + mark + "</td>" + cellsToTD(cells, true) + "</tr>");
						rowsOfCurrQ.add(im);
					}
				}
				else {
					out.println("<tr><td>" + mark + "</td>" + cellsToTD(cells, true) + "</tr>");
					rowsOfCurrQ.add(im);
				}
				
				if (isEOF) break;
			}
			
			out.println("</table>");
			
			if (dryRun) {
				out.println("<a href='processData?f=" + fileIdx + "&dryRun=false&action=import&filename="+getFile.getFileName()+"' class='btn btn-danger'>" + MessageUtil.getMessage(locale, "global.import") + "</a>");
			}
			else {
				out.println("<h2>Updated docs: " + updatedCount + "</h2>");
			}
		}
		catch (Exception e) {
			out.println("<div class='alert alert-danger'><h2>處理到第" + rowCount + "列出現錯誤，無法繼續處理：" + e.getMessage() + "<br>" + cells + "</h2></div>");
			e.printStackTrace();
		}
	}

	public static String cellsToTD(List<String> cells) {
		return cellsToTD(cells, false);
	}

	public static String cellsToTD(List<String> cells, boolean nullToEmpty) {
		StringBuffer b = new StringBuffer();
		for (String cell: cells) {
			b.append("<td>");
			if (nullToEmpty)
				b.append(StringUtils.trimToEmpty(cell));
			else 
				b.append(cell);
				
			b.append("</td>");
		}
		
		return b.toString();
	}

	public static String[] getQaDataHeaders(ICsvListReader listReader) throws IOException {
		List<String> tmpL = listReader.read();

		tmpL = ProcessQADataServlet.truncateTailNulls(tmpL);
		System.out.println(tmpL);

		if (MessageUtil.getMessage(Locale.TAIWAN, "knowledge.inventory.table").equals(WiSeUtils.cn2tw(StringUtils.trim(tmpL.get(tmpL.size() - 1))))) {
			listReader.read(); // Skip 2rd row
			tmpL = listReader.read();
		}

		int pos = 0;
		for (int i=0; i < tmpL.size() && tmpL.get(i) != null; i++,pos++)
		{}

		String[] headers = tmpL.subList(0,  pos).toArray(new String[0]); // Correct header

		for (int i=0; i < headers.length; i++) {
			String org = StringUtils.trimToEmpty(headers[i]);
			String tw = WiSeUtils.cn2tw(org);
			if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "standard.problem")) || StringUtils.equalsIgnoreCase(tw, "question")) {
				headers[i] = "QUESTION";
			}
			else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "other.questions")) || StringUtils.equalsIgnoreCase(tw, "question_alt")) {
				headers[i] = "QUESTION_ALT(m)";
			}
			else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "smart.robot.knowledge.no")) || StringUtils.equalsIgnoreCase(tw, "id")) {
				headers[i] = "id";
			}
			else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "main.classification")) || StringUtils.equalsIgnoreCase(tw, "category")) {
				headers[i] = "CATEGORY(m)";
			}
			else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "secondary.classification"))) {
				headers[i] = "CATEGORY(m)";
			}
			else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "over.secondary.classification"))) {
				headers[i] = "CATEGORY(m)";
			}
			else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "global.answer")) || StringUtils.equalsIgnoreCase(tw, "answer")) {
				headers[i] = "ANSWER";
			}
			else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "advanced.answer")) || StringUtils.equalsIgnoreCase(tw, "answer_richtext")) {
				headers[i] = "ANSWER_RICHTEXT";
			}
			else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "external.link")) || StringUtils.equalsIgnoreCase(tw, "answer_link")) {
				headers[i] = "ANSWER_LINK";
			}
			else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "knowledge.point")) || StringUtils.equals(tw, "knowledge_point")) {
				headers[i] = "KNOWLEDGE_POINT";
			}
			else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "template.number")) || StringUtils.equals(tw, "alt_template")) {
				headers[i] = "ALT_TEMPLATE";
			}
			else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "hierarchical.qa")) || StringUtils.equals(tw, "hierarchical_qa")) {
				headers[i] = "HIERARCHICAL_QA";
			}
			else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "global.plug")) || StringUtils.equals(tw, "answer_plugin_id")) {
				headers[i] = "ANSWER_PLUGIN_ID";
			}
			else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "not.recommended")) || StringUtils.equals(tw, "not_for_mlt")) {
				headers[i] = "IsNotForMlt";
			}
			else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "not.selected")) || StringUtils.equals(tw, "not_for_search")) {
				headers[i] = "IsNotForSearch";
			}
			else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "no.additional.recommendation")) || StringUtils.equals(tw, "not_append_mlt")) {
				headers[i] = "IsNotAppendMlt";
			}
			else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "global.disable")) || StringUtils.equals(tw, "banned")) {
				headers[i] = "IsBanned";
			}
			else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "recommended.display.problem")) || StringUtils.equals(tw, "Briefly_QUESTION")) {
				headers[i] = "Briefly_QUESTION";
			}
			else if (tw.startsWith("AUTOGEN")) {
				headers[i] = "AUTOGEN_s";
			}
			else if (tw.startsWith("qaCategory")) {
				headers[i] = "QA_CATEGORY_s";
			}
			else if (org.startsWith("delete mark")) {
				headers[i] = "DELETE_MARK_ig";
			}
			else if(tw.startsWith("TestCases")) {
				headers[i] = "TestCases";
			}
			else if (tw.startsWith("繼承例句範本") || StringUtils.equals(tw, "INHERITANT_ALT_TEMPLATE")) {
				headers[i] = "INHERITANT_ALT_TEMPLATE(m)";
			}
			else if (tw.startsWith("answer_") && tw.split("_").length == 3) {
				//ignore may be multichannelanswer
			}
			else if (tw.startsWith("answer_") && tw.split("_").length == 4) {
				//it is multichannelanswer with lineMKey
			}
			else if (tw.startsWith("qaBinder")) {
				headers[i] = "BINDER_ID_ml";
			}
			else {
				headers[i] = tw + "(ig)";
			}
			System.out.println(String.format("[%s]=[%s]", org, headers[i]));
		}
		return headers;
	}

	public static String[] getNonJsonHierarchicalQaDataHeaders(ICsvListReader listReader) throws IOException {
		List<String> tmpL = listReader.read();
		listReader.read();

		tmpL = ProcessQADataServlet.truncateTailNulls(tmpL);
		System.out.println(tmpL);

		if (MessageUtil.getMessage(Locale.TAIWAN, "knowledge.inventory.table").equals(WiSeUtils.cn2tw(StringUtils.trim(tmpL.get(tmpL.size() - 1))))) {
			listReader.read(); // Skip 2rd row
			tmpL = listReader.read();
		}

		int pos = tmpL.size();
		for (int i=tmpL.size() - 1; i >= 0 && tmpL.get(i) == null; i--,pos--)
		{}

		String[] headers = tmpL.subList(0,  pos).toArray(new String[0]); // Correct header
		String [] args = {"([0-9]+)"};
		Pattern p = Pattern.compile(MessageUtil.getMessage(Locale.TAIWAN, "zeor.to.nine.hierarchical.question",args));
		int currentLv = 0;

		for (int i=0; i < headers.length; i++) {
			String org = headers[i];
			System.out.println(org);
			String [] args1 = {"%d"};
			if (org == null) {
				headers[i] = String.format(MessageUtil.getMessage(Locale.TAIWAN, "zeor.to.nine.hierarchical.question",args1)+"-%s", currentLv, MessageUtil.getMessage(Locale.TAIWAN, "global.select.option"));
			}
			else {
				String tw = WiSeUtils.cn2tw(headers[i]);
				Matcher m = p.matcher(org);
				if (m.find()) {
					currentLv = Integer.parseInt(m.group(1));
					headers[i] = String.format(MessageUtil.getMessage(Locale.TAIWAN, "zeor.to.nine.hierarchical.question",args1)+"-%s", currentLv, MessageUtil.getMessage(Locale.TAIWAN, "fixed.text"));
				}
				else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "whether.to.end"))) {
					headers[i] = "EOL";
				}
				else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "standard.problem")) || StringUtils.equals(tw, "question")) {
					headers[i] = "QUESTION";
				}
				else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "other.questions")) || StringUtils.equals(tw, "question_alt")) {
					headers[i] = "QUESTION_ALT(m)";
				}
				else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "smart.robot.knowledge.no")) || StringUtils.equals(tw, "id") || StringUtils.equals(org, "No")) {
					headers[i] = "id";
				}
				else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "main.classification")) || StringUtils.equals(tw, "category")) {
					headers[i] = "CATEGORY(m)";
				}
				else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "secondary.classification"))) {
					headers[i] = "CATEGORY(m)";
				}
				else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "over.secondary.classification"))) {
					headers[i] = "CATEGORY(m)";
				}
				else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "global.answer")) || tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "global.solution")) || StringUtils.equals(tw, "answer")) {
					headers[i] = "ANSWER";
				}
				else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "advanced.answer")) || StringUtils.equals(tw, "answer_richtext")) {
					headers[i] = "ANSWER_RICHTEXT";
				}
				else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "external.link")) || StringUtils.equals(tw, "answer_link")) {
					headers[i] = "ANSWER_LINK";
				}
				else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "hierarchical.qa"))) {
					headers[i] = "HIERARCHICAL_QA";
				}
				else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "not.recommended")) || StringUtils.equals(tw, "not_for_mlt")) {
					headers[i] = "IsNotForMlt";
				}
				else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "not.selected")) || StringUtils.equals(tw, "not_for_search")) {
					headers[i] = "IsNotForSearch";
				}
				else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "no.additional.recommendation")) || StringUtils.equals(tw, "not_append_mlt")) {
					headers[i] = "IsNotAppendMlt";
				}
				else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "global.disable")) || StringUtils.equals(tw, "banned")) {
					headers[i] = "IsBanned";
				}
				else if (tw.startsWith(MessageUtil.getMessage(Locale.TAIWAN, "recommended.display.problem")) || StringUtils.equals(tw, "Briefly_QUESTION")) {
					headers[i] = "Briefly_QUESTION";
				}
				else if (tw.startsWith("例句") || StringUtils.equals(tw, "TestCases")) {
					headers[i] = "TestCases";
				}
				else if (tw.startsWith("繼承例句範本") || StringUtils.equals(tw, "INHERITANT_ALT_TEMPLATE")) {
					headers[i] = "INHERITANT_ALT_TEMPLATE(m)";
				}
				else if (tw.startsWith("answer_") && tw.split("_").length == 3) {
					//ignore may be multichannelanswer
				}
				else {
					headers[i] = tw + "(ig)";
				}
			}
			//System.out.println(String.format("[%s]=[%s]", org, headers[i]));
		}
		return headers;
	}

	String[] schemaTypes = {"t", "s", "l", "i", "b", "d", "dt", "geo"};

	private boolean needGuessFieldType(String cn) {
		for (String schemaType: schemaTypes) {
			if (StringUtils.endsWith(cn, "_" + schemaType)) {
				return false;
			}
			if (StringUtils.endsWith(cn, "_m" + schemaType)) {
				return false;
			}
		}
		return true;
	}

	private String getFieldNamePrefix(String cn) {
		if (StringUtils.contains(cn, "(m)")) {
			cn = StringUtils.replace(cn, "(m)", "");
		}
		else {
			for (String schemaType: schemaTypes) {
				if (StringUtils.endsWith(cn, "_" + schemaType)) {
					cn = StringUtils.substringBeforeLast(cn, "_" + schemaType);
				}
				if (StringUtils.endsWith(cn, "_m" + schemaType)) {
					cn = StringUtils.substringBeforeLast(cn, "_m" + schemaType);
				}
			}
		}

		cn = cn.replaceAll("[^a-z_A-Z0-9]", "");

		return cn;
	}

	private boolean isMultiValue(String cn) {
		boolean isMV = false;

		if (StringUtils.contains(cn, "(m)")) {
			isMV = true;
		}
		else {
			for (String schemaType: schemaTypes) {
				if (StringUtils.endsWith(cn, "_m" + schemaType)) {
					isMV = true;
				}
			}
		}

		return isMV;
	}

	public static String[] getFieldName(String prefix, boolean isMV) {

		if (StringUtils.equalsIgnoreCase(prefix, "id"))
			return new String[] {"id"};
		else if (StringUtils.equalsIgnoreCase(prefix, "ANSWER_PLUGIN_ID"))
			return new String[] {"ANSWER_PLUGIN_ID_s"};
		else if (StringUtils.equalsIgnoreCase(prefix, "ALT_TEMPLATE"))
			return new String[] {"APPLIED_ALT_TEMPLATE_ms"};
		else if (StringUtils.equalsIgnoreCase(prefix, "INHERITANT_ALT_TEMPLATE"))
			return new String[] {"INHERITANT_ALT_TEMPLATE_ms"};
		else if (StringUtils.equalsIgnoreCase(prefix, "KNOWLEDGE_POINT"))
			return new String[] {"KNOWLEDGE_POINT_ms"};
		else if (StringUtils.equalsIgnoreCase(prefix, "IsNotForMlt"))
			return new String[] {QA.FN_IS_NOT_FOR_MLT};
		else if (StringUtils.equalsIgnoreCase(prefix, "IsNotForSearch"))
			return new String[] {QA.FN_IS_NOT_FOR_SEARCH};
		else if (StringUtils.equalsIgnoreCase(prefix, "IsNotAppendMlt"))
			return new String[] {QA.FN_NOT_APPEND_MLT};
		else if (StringUtils.equalsIgnoreCase(prefix, "IsBanned"))
			return new String[] {QA.FN_BANNED};
		else if(StringUtils.equalsIgnoreCase(prefix, "Briefly_QUESTION")) {
			return new String[] {QA.FN_BRIEFLY_QUESTION};
		}
		return isMV ? new String[] {prefix + "_ms", prefix + "_mt"}
					: new String[] {prefix + "_s", prefix + "_t"};
	}
	
	public static boolean isQaFile(String fileName) {
		String twFileName = WiSeUtils.cn2tw(fileName);
		return StringUtils.startsWith(twFileName, ProcessQADataServlet.QA_DATA_FILE_NAME) || StringUtils.startsWith(twFileName, ProcessQADataServlet.CASUAL_DATA_FILE_NAME);
	}
}
