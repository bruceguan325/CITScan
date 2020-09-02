package com.intumit.quartz;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.elasticsearch.common.base.Stopwatch;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.InterruptableJob;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.FmtDate;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.exception.SuperCsvCellProcessorException;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.CsvContext;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.robot.AutoEvaluationLog;
import com.intumit.solr.robot.EvaluationLogEntity;
import com.intumit.solr.robot.QAEvaluationLog;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.tenant.Apikey;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.EmailUtil;
import com.intumit.solr.util.WiSeEnv;
import com.intumit.solr.util.WiSeUtils;
import com.intumit.util.PoiUtil;
import com.mchange.io.FileUtils;
import com.thoughtworks.xstream.XStream;

/**
 *
 * NonIndexJob.JOB_DETAIL_PROPERTIES[0] ==> Additional Filter Query
 *
 * @author Herb
 */
public class AutoEvaluationJob extends NonIndexJob implements InterruptableJob {
	public static final String[] JOB_DETAIL_PROPERTIES = {"p1", "p2", "p3", "p4"};

	private static final Logger LOG = LoggerFactory.getLogger(AutoEvaluationJob.class);
	private boolean killSignal;
	private ScheduleLogEntity scheduleLog = null;
	private Long scheduleTimestamp = null;
	private List<String> qaApiUrls = null;
	Stopwatch sw = null;

	private String scheduleNote;

	String toXML(Object rsp) {
		XStream outXML = new XStream();
		outXML.autodetectAnnotations(true);
		String out = outXML.toXML(rsp);
		return out;
	}

	/*
 $.ajax({
   		url: '<%= request.getContextPath() %>/qa-ajax.jsp',
   		dataType: 'json',
   		async: false,
   		data: {
     		  q: message,
     		  testMode: true,
    		  tid: <%= t.getId() %>,
      		  html: true
   		},
   		error: function() {
   			alert('無法進行測試，請檢測網路或者是否仍在登入狀態');
   		},
   		success: function(resp) {
   			var t = moment((new Date()).getTime());
				var formatted = t.format("YYYY/MM/DD HH:mm:ss");
				var kid = null;
				var addClass = "text-primary";
				if (resp.hasOwnProperty("kid")) {
					kid = resp.kid;
				}
				if ((isNaN($correctAnswerId) && $robotAnswerId == kid) || $correctAnswerId == kid) {
					addClass = "text-success glyphicon glyphicon-ok-circle";
					$stat.good++;
				}
				else if (isNaN($correctAnswerId) && isNaN($robotAnswerId)){
					addClass = "text-warning glyphicon glyphicon-question-sign";
					$stat.unknown++;
				}
				else {
					addClass = "text-danger glyphicon glyphicon-remove-circle";
					$stat.bad++;
				}

				var tr = $('<tr/>');
				$('<td><h3></h3></td><td/>').appendTo(tr);
				$('<td><span class="glyphicon glyphicon-arrow-up" aria-hidden="true"></span><span class="glyphicon glyphicon-arrow-up" aria-hidden="true"></span><span class="glyphicon glyphicon-arrow-up" aria-hidden="true"></span></td>').appendTo(tr);
				$('<td></td>').appendTo(tr);
				$('<td/>').append($('<h3/>').append(getQuestionLink(kid, addClass))).appendTo(tr);
				$('<td>' + resp.output + '</td>').addClass('answer-text').appendTo(tr);
				$('<td>' + (resp.hasOwnProperty('confidence') ? resp.confidence : '') + '</td>').appendTo(tr);
				$('<td class="text-danger timeago" title="' + formatted + '">' + formatted + '</td>').appendTo(tr);
				$('<td></td>').appendTo(tr);

				tr.insertAfter($insertAfterThisTr);
     		},
   		complete: function() {
   			setTimeout(function() {$('.loading-panel').hide();}, 300);
				$(".timeago").timeago();
   		}
    });
	 */

	/*
	 *
	 * (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		killSignal = false;
		JobDetail jobDetail = context.getJobDetail();

		scheduleLog = new ScheduleLogEntity();
		scheduleLog.setEventSource(jobDetail.getName());
		scheduleLog.setParameters(jobDetail.getJobDataMap().toString());
		Trigger trigger = context.getTrigger();

		scheduleTimestamp = System.currentTimeMillis();
		LOG.info("Start " + trigger.getFullName() + " - " + new java.util.Date());
		try {
			int tenantId = jobDetail.getJobDataMap().getIntegerFromString(NonIndexJob.JOB_DETAIL_PROPERTIES[0]);
			String qaApiUrlsStr = jobDetail.getJobDataMap().getString(NonIndexJob.JOB_DETAIL_PROPERTIES[1]);
			String options = jobDetail.getJobDataMap().getString(NonIndexJob.JOB_DETAIL_PROPERTIES[2]);
			String emails = jobDetail.getJobDataMap().getString(NonIndexJob.JOB_DETAIL_PROPERTIES[3]);
			JSONObject optionsJson = new JSONObject(options);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			
			Tenant t = Tenant.get(tenantId);
			Integer admId = optionsJson.has("admId") ? optionsJson.optInt("admId") : null;
			Integer numOfThreads = optionsJson.has("threads") ? optionsJson.optInt("threads") : 4;
			Long kid = optionsJson.has("kid") ? optionsJson.getLong("kid") : null;
			Integer flag = optionsJson.has("flag") ? optionsJson.optInt("flag") : null;
			Integer lastTestResult = optionsJson.has("ltr") ? optionsJson.optInt("ltr") : null;
			String timeRange = optionsJson.has("timeRange") ? optionsJson.optString("timeRange") : null;
			String bypassAdmIds = optionsJson.has("bypassAdmIds") ? optionsJson.optString("bypassAdmIds") : null;
			Integer limit = optionsJson.has("limit") ? optionsJson.optInt("limit") : null;
			boolean distinctQuesiton = optionsJson.optBoolean("dq", false);
			boolean dontEmail = optionsJson.optBoolean("dontEmail", false);
			boolean attachLtrDetail = optionsJson.optBoolean("ltrDetail", false);
			JSONArray thoseCategoriesOnly = optionsJson.optJSONArray("thoseCategoriesOnly");
			scheduleNote = optionsJson.has("note") ? optionsJson.optString("note") : null;
			
			Date afterDate = null;
			Date beforeDate = null;

			if (timeRange != null) {
				Date[] timeRangePair = WiSeUtils.getTimeRangePair(timeRange);
				afterDate = timeRangePair[0];
				beforeDate = timeRangePair[1];
			}
			else {
				afterDate = optionsJson.has("afterDate") ? sdf.parse(optionsJson.getString("afterDate")) : null;
				beforeDate = optionsJson.has("beforeDate") ? sdf.parse(optionsJson.getString("beforeDate")) : null;
			}
			
			Map<Integer, String> qaCategoryMap = t.getQaCategoryMap();
			
			// 如果有設定只跑特定分類，那麼就清掉不允許的分類
			if (thoseCategoriesOnly != null) {
				Set<Integer> toBeRemove = new HashSet<Integer>();
				
				for (Integer qaCategoryIdx : qaCategoryMap.keySet()) {
					String qaCategory = qaCategoryMap.get(qaCategoryIdx);
					boolean allowed = false;
					
					for (int i=0; i < thoseCategoriesOnly.length(); i++) {
						String allowedQaCategory = thoseCategoriesOnly.getString(i);
						
						if (StringUtils.equals(allowedQaCategory, qaCategory))
							allowed = true;
					}
					
					if (!allowed) {
						toBeRemove.add(qaCategoryIdx);
					}
				}
				
				for (Integer qaCategoryIdx: toBeRemove) {
					qaCategoryMap.remove(qaCategoryIdx);
				}
			}
			
			qaApiUrls = QAUtil.parseMultiValue(qaApiUrlsStr);
			int apiMark = -1;
			int totalAll = 0, counterAll = 0;
			long elapsedMillisAll = 0;
			
			for (String qaApiUrl: qaApiUrls) {
				sw  = Stopwatch.createStarted();
				
				apiMark++;
				AtomicInteger[][][] qaCategoryFlagStats = new  AtomicInteger[qaCategoryMap.keySet().size()][7][];
				ExecutorService executor = Executors.newFixedThreadPool(numOfThreads);
				AtomicInteger totalThisApi = new AtomicInteger(0);
				AtomicInteger counterThisApi = new AtomicInteger(0);
	
				for (Integer qaCategoryIdx : qaCategoryMap.keySet()) {
					if (killSignal) break;
					String qaCategory = qaCategoryMap.get(qaCategoryIdx);
					if (qaCategoryFlagStats[qaCategoryIdx] == null) {
						qaCategoryFlagStats[qaCategoryIdx] = new AtomicInteger[7][];
					}
					AtomicInteger[][] flagStats = qaCategoryFlagStats[qaCategoryIdx];
					int total = EvaluationLogEntity.countBy(t.getId(), QAUtil.CURRENT_EVALUATION_LOG_NS, null, kid, flag, lastTestResult, null, null, null, distinctQuesiton, qaCategoryMap.get(qaCategoryIdx), bypassAdmIds, afterDate, beforeDate).intValue();
					if (limit != null && total > limit) 
						total = limit;
					
					totalThisApi.addAndGet(total);
					
			        int rows = 100;
					int start = 0;
					
					if (start + rows > total) {
						rows = total - start;
					}
	
					loopWhile: while (start < total) {
						if (killSignal) break;
	
						List<EvaluationLogEntity> logs = EvaluationLogEntity.listBy(t.getId(), QAUtil.CURRENT_EVALUATION_LOG_NS, null, kid, flag, lastTestResult, null, null, null, distinctQuesiton, qaCategory, bypassAdmIds, afterDate, beforeDate, start, rows);
						int offset = 0;
	
						for (EvaluationLogEntity log: logs) {
							if (killSignal) break loopWhile;
							if (limit != null && (start+offset) >= limit) break loopWhile;
	
							int currFlag = log.getFlag();
	
							if (flagStats[currFlag] == null) {
								synchronized (flagStats) {
									if (flagStats[currFlag] == null) {
										flagStats[currFlag] = new  AtomicInteger[] {new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0)}; // good, bad, unknown
									}
								}
							}
	
							AtomicInteger[] stat = flagStats[currFlag];
	
							if (currFlag == 0) {
								stat[2].incrementAndGet();
								continue;
							}
	
						    Runnable worker = new MyRunnable(qaApiUrl, apiMark, t, log, stat, counterThisApi, totalThisApi);
						    executor.execute(worker);
						}
	
			            start += rows;
					}
				}
	
			    // This will make the executor accept no new threads
			    // and finish all existing threads in the queue
			    executor.shutdown();
			    // Wait until all threads are finish
			    executor.awaitTermination(24, TimeUnit.HOURS);
			    long elapsedMillisecond = sw.elapsed(TimeUnit.MILLISECONDS);
	
			    if (!dontEmail) {
					List<MimeBodyPart> attachs = new ArrayList<MimeBodyPart>();
					
					// 準備精準度報告附檔
					byte[] pdfBA = generateExcel(t, qaCategoryMap, qaCategoryFlagStats, optionsJson);
					SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd_HHmm");
					String subject = String.format("自動測試報告 %s (%s ~ %s) (APIMARK:%d)", t.getName(), afterDate != null ? sdf.format(afterDate) : "*", beforeDate != null ? sdf.format(beforeDate) : "*", apiMark);
					String filename = "自動測試報告_" + t.getName() + "_(產生時間" + sdf2.format(new Date()) + ")_" + apiMark + ".xls";
					
					attachs.add(EmailUtil.toAttachment(pdfBA, "application/vnd.ms-excel", filename));
					
					// 準備詳細測試紀錄附檔 (這類型檔案通常很大，會 zip 在同一個檔案當中，但是各個郵件信箱的大小限制無法在這裡控制，也無法預先得知）
					if (attachLtrDetail) {
						File tmpZipFile = File.createTempFile("zip" + System.currentTimeMillis(), ".zip");
						FileOutputStream tmpZipFileOs = new FileOutputStream(tmpZipFile);
						ZipOutputStream zos = new ZipOutputStream(tmpZipFileOs);
						
						for(Integer qaCategoryIdx : qaCategoryMap.keySet()) {
							String qaCategory = qaCategoryMap.get(qaCategoryIdx);
							
							try {
								File tmpFile = File.createTempFile("" + System.currentTimeMillis(), ".csv");
								FileWriter tmpFileWriter = new FileWriter(tmpFile);
								generateDetailLogCsv(tmpFileWriter, true, t, admId, kid, flag, lastTestResult, afterDate, beforeDate, distinctQuesiton, qaCategory, bypassAdmIds, limit);
								
								tmpFileWriter.close();
								String fileName = "詳細報告_分類_" + qaCategory + ".csv";
								addToZipFile(tmpFile, fileName, zos);
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}
						
		
						zos.close();
						byte[] detailZip = FileUtils.getBytes(tmpZipFile);
						
						attachs.add(EmailUtil.toAttachment(detailZip, "application/zip", "詳細報告_" + apiMark + ".zip"));
					}
		
					if (killSignal) {
						subject = "[已中斷]" + subject;
					}
					
					FileOutputStream fos = new FileOutputStream(WiSeEnv.getHomePath().replace("kernel", "") + "webapps" + File.separator + "wise" + File.separator
							+ File.separator + "commons" + File.separator + sdf2.format(new Date()) + "_auto.xls");
			        fos.write(pdfBA);
			        fos.flush();
			        fos.close();
		
					// 準備信件內文
					String html = String.format("<html><body>自動測試報告<br>公司：%s (%s) <br>API URL (%s)<br>時間範圍 (%s ~ %s)<br></body></html>", t.getName(), t.getNotes(), qaApiUrl, afterDate != null ? sdf.format(afterDate) : "*", beforeDate != null ? sdf.format(beforeDate) : "*");
		
					EmailUtil.sendmail(InternetAddress.parse(emails, false), InternetAddress.parse("herb@intumit.com", false), subject, html, attachs.toArray(new MimeBodyPart[0]));
			    }
			    else {
			    	printToConsole(t, qaCategoryMap, qaCategoryFlagStats, optionsJson);
			    }
			    
			    totalAll += totalThisApi.get();
			    counterAll += counterThisApi.get();
			    elapsedMillisAll += elapsedMillisecond;
			}
			
			double avgQtime = (int)(elapsedMillisAll * 100  / counterAll) / 100; 
			String statusMsg = "End (" + counterAll + "/" + totalAll + ") 100% elasped " + (int)(elapsedMillisAll/1000) + "s / avg (" + avgQtime + "ms) - " + new java.util.Date();
			scheduleLog.setStatusMessage(statusMsg);

			ScheduleLogFacade.save(scheduleLog);
			LOG.info(statusMsg);
			
		} catch (Exception e) {
			LOG.error("Error", e);
			String statusMsg = "Exception End  " + trigger.getFullName() + " - " + new java.util.Date();

			LOG.error(statusMsg, e);
			scheduleLog.setStatusMessage(statusMsg);
			scheduleLog.setErrors(e.getMessage());

			ScheduleLogFacade.save(scheduleLog);
		}
	}

	public class MyRunnable implements Runnable {
		private String qaApiUrl;
		private EvaluationLogEntity log;
		private AtomicInteger[] stat;
		private Tenant t;
		private AtomicInteger counter;
		private AtomicInteger total;
		private int apiMark;

		public MyRunnable(String qaApiUrl, int apiMark, Tenant t, EvaluationLogEntity log, AtomicInteger[] stat, AtomicInteger counter, AtomicInteger total) {
			this.qaApiUrl = qaApiUrl;
			this.apiMark = apiMark;
			this.t = t;
			this.log = log;
			this.stat = stat;
			this.counter = counter;
			this.total = total;
			stat[2].incrementAndGet(); // 先新增一筆 unknown，跑完有結果再減去，不然好像常常會有不見的
		}

		@Override
		public void run() {
			if (killSignal) return;

			int currFlag = log.getFlag();

			try {
				long begin = System.currentTimeMillis();
				String url = qaApiUrl + "?q=" + URLEncoder.encode(log.getQuestion(), "UTF-8")
						+ "&testMode=true"
						+ "&ftc=false"
						+ "&tid=" + t.getId()
						+ "&html=false"
						+ "&qaCategorySelect=" + URLEncoder.encode(log.getQaCategory(), "UTF-8");

				// 若有 APIKEYS，取第一個
				List<Apikey> apikeys = Apikey.list(t.getId());
				if (apikeys.size() > 0) {
					url += "&apikey=" + apikeys.get(0).getApikey();
				}
				else {
					System.out.println("Tenant [" + t.getName() + " (" + t.getId() + ")] has no apikey, the auto evaluation job be killed now.");
					killSignal = true;
					return;
				}

				String respStr = WiSeUtils.getDataFromUrl(url);
				long qtime = System.currentTimeMillis() - begin;
				
				JSONObject resp = new JSONObject(respStr);

				Long currKid = resp.has("kid") ? resp.optLong("kid") : null;
				Long correctAnswerId = log.getCorrectAnswerId();
				Long robotAnswerId = log.getRobotAnswerId();
				Integer ltr = QAEvaluationLog.judgeCorrectOrNot(currFlag, correctAnswerId, robotAnswerId, currKid);
				
				if (ltr != null) {
					if (ltr == 1) {
						stat[2].decrementAndGet();
						stat[0].incrementAndGet();
					}
					else if (ltr == 0) {
						stat[2].decrementAndGet();
						stat[1].incrementAndGet();
					}
				}

				if (ltr != null) {
					log.setLastTestResult(ltr);
					log.setLastTestResultTimestamp(Calendar.getInstance().getTime());
					if (resp.has("currentQaCategory")) {
						log.setQaCategory(resp.getString("currentQaCategory"));
					}
					
					if (currKid !=null) {
					    log.setLastRobotAnswerId(currKid); 
					}
					EvaluationLogEntity.update(log);
					AutoEvaluationLog.log(log, scheduleTimestamp, apiMark, scheduleNote == null ? "N/A" : scheduleNote, ltr, resp, qtime);
				}
			}
			catch (Exception e) {
				System.out.println("Error during AutoEvaluationJob: EvaluationLogEntity ID [" + log.getId() + " / " + log.getQuestion() + "]:" + e.getMessage() );
				e.printStackTrace();
			}
			finally {
				try {
					int val = counter.incrementAndGet();
					float percent = Math.round((val * 10000f) / total.get()) / 100;
					String statusMsg = "Progress " + apiMark + " of " + qaApiUrls.size() + " (" + val + "/" + total.get() + ") " + percent + "% elasped " + sw.elapsed(TimeUnit.SECONDS) + "s - " + new java.util.Date();
					scheduleLog.setStatusMessage(statusMsg);
					ScheduleLogFacade.save(scheduleLog);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static byte[] generateExcel(Tenant tenant, Map<Integer, String> qaCateogryMap, AtomicInteger[][][] qaCategoryFlagStats, JSONObject options) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		HSSFWorkbook workbook = new HSSFWorkbook();
		CreationHelper createHelper = workbook.getCreationHelper();

		HSSFFont font = workbook.createFont();
		font.setColor(IndexedColors.WHITE.getIndex());
		//font.setFontHeightInPoints((short)28);
		font.setBold(true);
		HSSFCellStyle style = addBorder(workbook.createCellStyle());

		HSSFCellStyle normalStyle = addBorder(workbook.createCellStyle());
		HSSFCellStyle dateCellStyle = addBorder(workbook.createCellStyle());
		String[] header = new String[] { "Flag", "正確", "錯誤", "未知", "總數", "準確度", };
		int[] headerWidth = new int[]  {    6,    10,    10,    6,     12,     12,     };

		int headerLength = header.length;

		{
			HSSFSheet sheet = workbook.createSheet("自動測試結果");
			style.setFont(font);
		    style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
		    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			int rownum = 0;
			for (int k=0; k < qaCategoryFlagStats.length; k++) {
				AtomicInteger[][] flagStats = qaCategoryFlagStats[k];

				{
					HSSFRow row = sheet.createRow(rownum++);
					int cellnum = 0;

					for (int i=0; i < headerLength; i++, cellnum++) {
						HSSFCell cell = row.createCell(cellnum);
						cell.setCellValue(header[i]);
						cell.setCellStyle(style);

						sheet.setColumnWidth(cellnum, headerWidth[i]*256);
					}
				}

				int[] totalStats = new int[] {0, 0, 0};
				for(int j = 0 ; j < flagStats.length ; j++) {
					AtomicInteger[] stats = flagStats[j]; // Good, Bad, Unknown

					if (stats == null) continue;

					if (j >= 1 && j <= 6 && j != 3) {
						totalStats[0] += stats[0].get(); totalStats[1] += stats[1].get(); totalStats[2] += stats[2].get();
					}

					HSSFRow row = sheet.createRow(rownum++);
					int cellnum = 0;

					for (int dN=0; dN < headerLength; dN++, cellnum++) {
						HSSFCell cell = row.createCell(cellnum);
						cell.setCellStyle(normalStyle);

						if ("Flag".equals(header[dN])) {
							cell.setCellValue( j );
						}
						else if ("正確".equals(header[dN])) {
							cell.setCellValue( stats[0].get() );
						}
						else if ("錯誤".equals(header[dN])) {
							cell.setCellValue( stats[1].get() );
						}
						else if ("未知".equals(header[dN])) {
							cell.setCellValue( stats[2].get() );
						}
						else if ("總數".equals(header[dN])) {
							cell.setCellValue( stats[0].get() + stats[1].get() + stats[2].get() );
						}
						else if ("準確度".equals(header[dN])) {
							cell.setCellValue( ((float)stats[0].get() * 100f) / (stats[0].get() + stats[1].get() + stats[2].get()) + "%" );
						}
					}
				}

				// 計算 Total
				HSSFRow row = sheet.createRow(rownum++);
				int cellnum = 0;

				for (int dN=0; dN < headerLength; dN++, cellnum++) {
					HSSFCell cell = row.createCell(cellnum);
					cell.setCellStyle(normalStyle);

					if ("Flag".equals(header[dN])) {
						cell.setCellValue( "Total" );
					}
					else if ("正確".equals(header[dN])) {
						cell.setCellValue( totalStats[0] );
					}
					else if ("錯誤".equals(header[dN])) {
						cell.setCellValue( totalStats[1] );
					}
					else if ("未知".equals(header[dN])) {
						cell.setCellValue( totalStats[2] );
					}
					else if ("總數".equals(header[dN])) {
						cell.setCellValue( totalStats[0] + totalStats[1] + totalStats[2] );
					}
					else if ("準確度".equals(header[dN])) {
						cell.setCellValue( ((float)totalStats[0] * 100f) / (totalStats[0] + totalStats[1] + totalStats[2]) + "%" );
					}
				}

				HSSFRow qaCategoryRow = sheet.createRow(rownum++);
				HSSFCell qaCategoryCell = qaCategoryRow.createCell(0);
				qaCategoryCell.setCellValue(String.format("類別: %s", qaCateogryMap.get(k)));
				//分隔
				sheet.createRow(rownum++);
			}
		}


		try {
			workbook.write(baos);
			baos.flush();
			System.out.println("Excel written successfully..");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return baos.toByteArray();
	}
	
	public static void printToConsole(Tenant tenant, Map<Integer, String> qaCateogryMap, AtomicInteger[][][] qaCategoryFlagStats, JSONObject options) {
		String[] header = new String[] { "Flag", "正確", "錯誤", "未知", "總數", "準確度", };
		int[] headerWidth = new int[]  {    6,    10,    10,    6,     12,     12,     };

		int headerLength = header.length;

		{
			System.out.println("<< 自動測試結果 >>");

			int rownum = 0;
			for (int k=0; k < qaCategoryFlagStats.length; k++) {
				System.out.println(String.format("類別: %s", qaCateogryMap.get(k)));
				
				AtomicInteger[][] flagStats = qaCategoryFlagStats[k];

				{
					int cellnum = 0;

					for (int i=0; i < headerLength; i++, cellnum++) {
						System.out.print(header[i]);
						System.out.print("\t");
					}
					
					System.out.println();
				}

				int[] totalStats = new int[] {0, 0, 0};
				for(int j = 0 ; j < flagStats.length ; j++) {
					AtomicInteger[] stats = flagStats[j]; // Good, Bad, Unknown

					if (stats == null) continue;

					if (j >= 1 && j <= 6 && j != 3) {
						totalStats[0] += stats[0].get(); totalStats[1] += stats[1].get(); totalStats[2] += stats[2].get();
					}

					int cellnum = 0;

					for (int dN=0; dN < headerLength; dN++, cellnum++) {
						Object val = null;

						if ("Flag".equals(header[dN])) {
							val = j;
						}
						else if ("正確".equals(header[dN])) {
							val = ( stats[0].get() );
						}
						else if ("錯誤".equals(header[dN])) {
							val = ( stats[1].get() );
						}
						else if ("未知".equals(header[dN])) {
							val = ( stats[2].get() );
						}
						else if ("總數".equals(header[dN])) {
							val = ( stats[0].get() + stats[1].get() + stats[2].get() );
						}
						else if ("準確度".equals(header[dN])) {
							val = ( ((float)stats[0].get() * 100f) / (stats[0].get() + stats[1].get() + stats[2].get()) + "%" );
						}
						
						System.out.print(val);
						System.out.print("\t");
					}
					
					System.out.println();
				}

				// 計算 Total
				int cellnum = 0;

				for (int dN=0; dN < headerLength; dN++, cellnum++) {
					Object val = null;

					if ("Flag".equals(header[dN])) {
						val = ( "Total" );
					}
					else if ("正確".equals(header[dN])) {
						val = ( totalStats[0] );
					}
					else if ("錯誤".equals(header[dN])) {
						val = ( totalStats[1] );
					}
					else if ("未知".equals(header[dN])) {
						val = ( totalStats[2] );
					}
					else if ("總數".equals(header[dN])) {
						val = ( totalStats[0] + totalStats[1] + totalStats[2] );
					}
					else if ("準確度".equals(header[dN])) {
						val = ( ((float)totalStats[0] * 100f) / (totalStats[0] + totalStats[1] + totalStats[2]) + "%" );
					}

					System.out.print(val);
					System.out.print("\t");
				}
				System.out.println();
			}
		}
	}
	
	public static void generateDetailLogCsv(Writer out, boolean exportDetails, Tenant t, Integer admId, Long kid, Integer flag, Integer lastTestResult, Date afterDate, Date beforeDate, boolean distinctQuesiton, String qaCategory, String bypassAdmIds, int limit) {
		List<EvaluationLogEntity> logs = new ArrayList<EvaluationLogEntity>();
	    ICsvBeanWriter beanWriter = null;
	    try {
	        beanWriter = new CsvBeanWriter(out, CsvPreference.EXCEL_PREFERENCE);

	        // the header elements are used to map the bean values to each column (names must match)
	        final String[] header =
	        		exportDetails
	        	? new String[] { "id", "admId", "clientIp", "flag", "correctAnswerId", "description", "robotAnswerId", "qaCategory", "question", "score", "originalTime", "timestamp", "conversations", "lastTestResult", "lastTestResultTimestamp", "eservice", "userType" }
	        	: new String[] { "id", "admId", "clientIp", "flag", "correctAnswerId", "description", "robotAnswerId", "qaCategory", "question", "score", "originalTime", "timestamp", "eservice", "userType" };

	        final CellProcessor[] processors =
	        		exportDetails
	        		? new CellProcessor[] {
	                        new NotNull(), // customerNo (must be unique)
	                        new AdminUserParser(), // admId
	                        new NotNull(), // clientIp
	                        new NotNull(), // flag
	                        new Optional(), // correctAnswerId
	                        new Optional(), // description
	                        new Optional(), // robotAnswerId
	                        new Optional(), // qaCategory
	                        new NotNull(), // question
	                        new NotNull(), // score
	                        new Optional(), // enterDate
	                        new FmtDate("yyyy-MM-dd HH:mm:ss"), // birthDate
	                        new ConversationParser("output"), // conversations
	                        new Optional(), // lastTestResult
	                        new FmtDate("yyyy-MM-dd HH:mm:ss"), // lastTestResultTimestamp
	                        new Optional(), // eservice
	                        new Optional(), // userType
	                	}
	        		: new CellProcessor[] {
	                        new NotNull(), // customerNo (must be unique)
	                        new AdminUserParser(), // admId
	                        new NotNull(), // clientIp
	                        new NotNull(), // flag
	                        new Optional(), // correctAnswerId
	                        new Optional(), // description
	                        new Optional(), // robotAnswerId
	                        new Optional(), // qaCategory
	                        new NotNull(), // question
	                        new NotNull(), // score
	                        new Optional(), // enterDate
	                        new FmtDate("yyyy-MM-dd HH:mm:ss"), // birthDate
	                        new Optional(), // eservice
	                        new Optional(), // userType
	                        //new ConversationParser("output") // conversations
	                	};

	        // write the header
	        beanWriter.writeHeader(header);

	        int total = EvaluationLogEntity.countBy(t.getId(), QAUtil.CURRENT_EVALUATION_LOG_NS, admId, kid, flag, lastTestResult, null, null, null, distinctQuesiton, qaCategory, bypassAdmIds, afterDate, beforeDate).intValue();
	        if (limit != -1 && total > limit)
	        	total = limit;

	        int rows = 100;
			int start = 0;

			while (start < total) {
				logs = EvaluationLogEntity.listBy(t.getId(), QAUtil.CURRENT_EVALUATION_LOG_NS, admId, kid, flag, lastTestResult, 
						null, 
						null, 
						null, 
						distinctQuesiton, 
						qaCategory, 
						bypassAdmIds,
						afterDate, beforeDate, start, start + rows > total ? (total - start) : rows);

	            // write the beans
	            for( final EvaluationLogEntity log : logs ) {
	                beanWriter.write(log, header, processors);
	            }

	            start += rows;
			}
	    }
	    catch(Exception ignore) {
	    	ignore.printStackTrace();
	    }
	    finally {
	        if( beanWriter != null ) {
                try {
					beanWriter.close();
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
	    }
	}

    public static void generateDetailLogXls(String[] header, List<?> list,
            OutputStream outPutStream) {
        XSSFWorkbook workBook = new XSSFWorkbook();
        PoiUtil.createSheet(workBook, header, list);
        Sheet sheet = workBook.getSheetAt(0);
        Row sheetHeader=workBook.getSheetAt(0).getRow(0);
        for(int i=0;i<=sheetHeader.getPhysicalNumberOfCells();i++) {
            sheet.autoSizeColumn(i);
        }
        try {
            workBook.write(outPutStream);
            outPutStream.flush();
            outPutStream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
	private static HSSFCellStyle addBorder(HSSFCellStyle style) {
		style.setBorderBottom(BorderStyle.MEDIUM);
		style.setBorderTop(BorderStyle.MEDIUM);
		style.setBorderRight(BorderStyle.MEDIUM);
		style.setBorderLeft(BorderStyle.MEDIUM);

		return style;
	}
	
	/**
	 * An example of a custom cell processor.
	 */
	public static class ConversationParser extends CellProcessorAdaptor {
		String targetField = null;

		public ConversationParser(String targetField) {
			super();
			this.targetField = targetField;
		}

		public ConversationParser(String targetField, CellProcessor next) {
			// this constructor allows other processors to be chained after this
			super(next);
			this.targetField = targetField;
		}

		public Object execute(Object value, CsvContext context) {
			validateInputNotNull(value, context);  // throws an Exception if the input is null

			try {
				JSONObject json = new JSONObject(value.toString());
				if (json.has(targetField))
					return next.execute(json.getString(targetField), context);
				else
					return next.execute("", context);
			}
			catch (Exception ignored) {}

			throw new SuperCsvCellProcessorException(
				String.format("Could not parse %s from '%s'", targetField, value), context, this);
		}
	}
	
	/**
	 * An example of a custom cell processor.
	 */
	public static class AdminUserParser extends CellProcessorAdaptor {
	
		public AdminUserParser() {
			super();
		}
	
		public AdminUserParser(CellProcessor next) {
			// this constructor allows other processors to be chained after this
			super(next);
		}
	
		public Object execute(Object value, CsvContext context) {
			validateInputNotNull(value, context);  // throws an Exception if the input is null
	
			try {
				Integer admId = new Integer(value.toString());
				AdminUser user = AdminUserFacade.getInstance().get(admId);
				if (user != null)
					return next.execute(user.getLoginName(), context);
				else
					return next.execute("", context);
			}
			catch (Exception ignored) {}
	
			throw new SuperCsvCellProcessorException(
				String.format("Could not parse admin id %s", value), context, this);
		}
	}
	
	public static void addToZipFile(File file, String fileNameInZip, ZipOutputStream zos) throws FileNotFoundException, IOException {
		FileInputStream fis = new FileInputStream(file);
		ZipEntry zipEntry = new ZipEntry(fileNameInZip);
		zos.putNextEntry(zipEntry);

		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) {
			zos.write(bytes, 0, length);
		}

		zos.closeEntry();
		fis.close();
	}

	private static String trimContent(String text) {
		if (StringUtils.length(text) > 32000) {
			return StringUtils.left(text, 32000);
		}
		return text;
	}

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		killSignal = true;
	}
}
