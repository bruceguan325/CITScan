package com.intumit.solr.robot;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.net.URLEncoder;
import java.sql.Blob;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.elasticsearch.common.base.Stopwatch;
import org.hibernate.Hibernate;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.intumit.quartz.AutoEvaluationJob;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.tenant.Apikey;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeUtils;
import com.intumit.systemconfig.WiseSystemConfig;
import com.intumit.util.PoiUtil;

public class QAEvaluationLogQueue {

	private static final Logger LOG = LoggerFactory.getLogger(QAEvaluationLogQueue.class);
	boolean killSignal = false;
	Stopwatch sw = null;
	private static Set<Integer> tenantWarmedUp = new HashSet<>();
	List<EvaluationLogEntity> totalEvaLogs = null;

	public static enum Status implements Serializable {

		PENDING("waite.treat"), PROCESSING("processing"), DONE("already.treat"), FAILED("detail.faile"),
		CANCELED("cancle.job"), UNKNOWN("unknown.state"),;

		public final String text;

		Status(String text) {
			this.text = text;
		}

	}

	static QAEvaluationLogQueue evaluationLogQueue = new QAEvaluationLogQueue();
	static QAEvaluationLog build;

	/**
	 * The document must be indexed before added into build queue. Bcz we need read
	 * the document from index for copying fields.
	 * 
	 * @param charset
	 * 
	 * @param blob
	 * 
	 * @param docId
	 * @param kid
	 * @param qAltTpls
	 */
	public static void add(Integer tenantId, Blob inputFile, String fileName, Date now, String charset) {
		QAEvaluationLog log = new QAEvaluationLog();
		log.setTenantId(tenantId);
		log.setInputFileName(fileName);
		log.setInputFile(inputFile);
		log.setStatus(Status.PENDING);
		log.setCreatedTime(now);
		log.setCharset(charset);

		evaluationLogQueue._add(log);
	}

	public static void startProcess() {
		for (QAEvaluationLog log : QAEvaluationLog.listByPenging()) {
			add(log.getTenantId(), log.getInputFile(), log.getInputFileName(), log.getCreatedTime(), log.getCharset());
		}
		evaluationLogQueue._startProcess();
	}

	public static void removePendingBuilds(QAEvaluationLog log) {
		evaluationLogQueue._removePendingBuilds(log);
	}

	public static synchronized void cancelBuild(long id) {
		if (build != null && build.getId() == id) {
			build.setStatus(Status.CANCELED);
		}
	}

	//
	// Non static area
	//
	private final LinkedBlockingQueue<QAEvaluationLog> queue = new LinkedBlockingQueue<QAEvaluationLog>();

	private final Set<MyRunner> processingRunners = new CopyOnWriteArraySet<MyRunner>();

	private final ExecutorService runner = Executors.newFixedThreadPool(2);// Executors.newSingleThreadExecutor();

	void _add(QAEvaluationLog log) {
		synchronized (queue) {
			removePendingBuilds(log);
			log.deleteByDocIdAndSave(); // 先清空並且儲存完再放到 queue 當中避免 queue 太快被 process 產生 racing condition
			queue.add(log);
		}
	}

	void _startProcess() {
		runner.execute(new MyRunner());
	}

	int getProcessingCount() {
		return processingRunners.size();
	}

	int getPendingCount() {
		return queue.size();
	}

	void _removePendingBuilds(QAEvaluationLog log) {
		Set<QAEvaluationLog> toRemoves = new HashSet<QAEvaluationLog>();
		for (QAEvaluationLog b : queue) {
			if (log.getInputFileName().equals(b.getInputFileName())) {
				toRemoves.add(b);
			}
		}
		queue.removeAll(toRemoves);
	}

	public class MyRunner implements Runnable {
		public MyRunner() {
			super();
		}

		@SuppressWarnings("deprecation")
		@Override
		public void run() {
			while (true) {
				build = null;
				try {
					build = queue.take();
					
					Integer tenantId = build.getTenantId();
					Tenant t = Tenant.get(tenantId);
					
					if (t == null || !t.getEnableTenant()) {
						LOG.info("Tenant (" + tenantId + ") disabled or not exist, skip running QAEvaluationLog (" + build.getInputFileName() + ").");
					}
					
					if (!tenantWarmedUp.contains(tenantId)) {
						LOG.info("Tenant (" + tenantId + ") need warmming up... do it now...");
						warmUp(build);
					}
					
					if (!tenantWarmedUp.contains(tenantId)) {
						continue;
					}
					
					LOG.info("Start executing QAEvaluationLog:[" + build.getInputFileName() + "] (" + build.getId() + " of Tenant " + tenantId + ")");

					ICsvListReader listReader = null;
					Map<String, List<EvaluationLogEntity>> maps = new HashMap<String, List<EvaluationLogEntity>>();
					List<String> qaCategories = new ArrayList<String>();
					totalEvaLogs = new ArrayList<EvaluationLogEntity>();

					try {
						Reader reader = new InputStreamReader(build.getInputFile().getBinaryStream(),
								build.getCharset());
						if (build.getInputFileName().endsWith(".csv")) {
							listReader = new CsvListReader(reader, CsvPreference.STANDARD_PREFERENCE);
						} else {
							listReader = new CsvListReader(reader, CsvPreference.TAB_PREFERENCE);
						}
						String[] headers = listReader.getHeader(true);
						List<String> cells = null;
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

						while ((cells = listReader.read()) != null) {

							EvaluationLogEntity log = new EvaluationLogEntity();

							for (int i = 0; i < headers.length && i < cells.size(); i++) {
								String header = headers[i];
								String cell = cells.get(i);

								if (StringUtils.trimToNull(cell) == null)
									continue;

								if (header.equals("id")) {
									log.setId(Integer.valueOf(cell));
								} else if (header.equals("admName")) {
									AdminUser user = AdminUserFacade.getInstance().getByLoginName(cell);
									if (user != null)
										log.setAdmId(user.getId());
								} else if (header.equals("clientIp")) {
									log.setClientIp(cell);
								} else if (header.equals("flag")) {
									log.setFlag(Integer.valueOf(cell));
								} else if (header.equals("correctAnswerId")) {
									log.setCorrectAnswerId(Long.valueOf(cell));
								} else if (header.equals("description")) {
									log.setDescription(cell);
								} else if (header.equals("robotAnswerId")) {
									log.setRobotAnswerId(Long.valueOf(cell));
								} else if (header.equals("qaCategory")) {
									log.setQaCategory(cell);
									if (!qaCategories.contains(cell)) qaCategories.add(cell);
								} else if (header.equals("question")) {
									log.setQuestion(cell);
								} else if (header.equals("originalTime")) {
									log.setOriginalTime(cell);
								} else if (header.equals("timestamp")) {
									try {
										log.setTimestamp(sdf.parse(cell));
									} catch (ParseException e) {
										LOG.info(e.toString());
									}
								} else if (header.equals("conversations")) {
									log.setConversations(cell);
								} else if (header.equals("lastRobotAnswerId") || header.equals("autoRobotAnswerId")) {
									log.setLastRobotAnswerId(Long.valueOf(cell));
								} else if (header.equals("lastTestResult")) {
									log.setLastTestResult(Integer.valueOf(cell));
								} else if (header.equals("lastTestResultTimestamp")) {
									try {
										log.setLastTestResultTimestamp(sdf.parse(cell));
									} catch (ParseException e) {
										LOG.info(e.toString());
									}
								} else if (header.equals("eservice")) {
									log.setEservice(cell);
								} else if (header.equals("userType")) {
									log.setUserType(cell);
								}

							}

							if (log.getId() != 0
									&& (log.getReviewStatus() != null || !log.getReviewStatus().equals(-1))) {
								// reviewStatus is null or eval.reviewStatus <> -1
								String qaCategory = log.getQaCategory();
								if (maps.containsKey(qaCategory)) {
									List<EvaluationLogEntity> logs = maps.get(qaCategory);
									logs.add(log);
									maps.put(qaCategory, logs);
								} else {
									List<EvaluationLogEntity> logs = new ArrayList<EvaluationLogEntity>();
									logs.add(log);
									maps.put(qaCategory, logs);
								}
								totalEvaLogs.add(log);
							}
						}
					} catch (Exception e) {
						// queue 有錯誤就暫時不管
						LOG.info(e.toString());
					} finally {
						if (listReader != null) {
							listReader.close();
						}
					}

					String qaApiUrl = WiseSystemConfig.get().getHostname()
									+ WiseSystemConfig.get().getContextPath() 
									+ "/qa-ajax.jsp";

					JSONObject optionsJson = new JSONObject().put("dq", true);
					Integer numOfThreads = 4;
					LOG.info("Starting " + numOfThreads + " threads to process QAEvaluationLog:[" + build.getInputFileName() + "] (" + build.getId() + " of Tenant " + tenantId + ")");

					Map<Integer, String> qaCategoryMap = new LinkedHashMap<Integer, String>();
					int idx = 0;
					for (String qaCategory : qaCategories) {
						 qaCategoryMap.put(idx++, qaCategory);
					}

					int apiMark = -1;
					int totalAll = 0, counterAll = 0;
					long elapsedMillisAll = 0;

					sw = Stopwatch.createStarted();

					apiMark++;
					AtomicInteger[][][] qaCategoryFlagStats = new AtomicInteger[qaCategoryMap.keySet().size()][7][];
					ExecutorService executor = Executors.newFixedThreadPool(numOfThreads);
					AtomicInteger totalThisApi = new AtomicInteger(0);
					AtomicInteger counterThisApi = new AtomicInteger(0);

					for (Integer qaCategoryIdx : qaCategoryMap.keySet()) {
						if (killSignal)
							break;
						String qaCategory = qaCategoryMap.get(qaCategoryIdx);
						if (qaCategoryFlagStats[qaCategoryIdx] == null) {
							qaCategoryFlagStats[qaCategoryIdx] = new AtomicInteger[7][];
						}
						AtomicInteger[][] flagStats = qaCategoryFlagStats[qaCategoryIdx];

						List<EvaluationLogEntity> logs = new ArrayList<EvaluationLogEntity>();
						if (maps.containsKey(qaCategory)) {
							logs = maps.get(qaCategory);
						}

						int total = logs.size();

						totalThisApi.addAndGet(total);

						int rows = 1000;
						int start = 0;

						if (start + rows > total) {
							rows = total - start;
						}

						loopWhile: while (start < total) {
							if (killSignal)
								break;

							for (EvaluationLogEntity log : logs.subList(start,
									(start + rows >= total) ? total : start + rows)) {
								if (killSignal)
									break loopWhile;

								int currFlag = log.getFlag();

								if (flagStats[currFlag] == null) {
									synchronized (flagStats) {
										if (flagStats[currFlag] == null) {
											flagStats[currFlag] = new AtomicInteger[] { new AtomicInteger(0),
													new AtomicInteger(0), new AtomicInteger(0) }; // good, bad,
																									// unknown
										}
									}
								}

								AtomicInteger[] stat = flagStats[currFlag];

								if (currFlag == 0) {
									stat[2].incrementAndGet();
									continue;
								}

								Runnable worker = new QaAjaxRunner(qaApiUrl, apiMark, t, log, stat, counterThisApi, totalThisApi, build, executor);
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
					if (build.getStatus() == Status.CANCELED) {
						continue;
					}

					// 準備精準度報告附檔
					byte[] pdfBA = AutoEvaluationJob.generateExcel(t, qaCategoryMap, qaCategoryFlagStats, optionsJson);
					if (killSignal)
						break;

					InputStream inputReport = new ByteArrayInputStream(pdfBA);
					Blob blobReport = Hibernate.createBlob(inputReport);
					build.setReportFile(blobReport);

					totalAll += totalThisApi.get();
					counterAll += counterThisApi.get();
					elapsedMillisAll += elapsedMillisecond;

					double avgQtime = (int) (elapsedMillisAll * 100 / counterAll) / 100;
					String statusMsg = "End (" + counterAll + "/" + totalAll + ") 100% elasped "
							+ (int) (elapsedMillisAll / 1000) + "s / avg (" + avgQtime + "ms) - "
							+ new java.util.Date();
					LOG.info(statusMsg);
					build.setStatusMsg(statusMsg);
					build.setProgress(100);
					
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					final String[] header = new String[] { "id", "admName", "clientIp", "flag", "correctAnswerId",
							"description", "robotAnswerId", "lastRobotAnswerId", "qaCategory", "question", "score", "originalTime",
							"timestamp", "conversations", "lastTestResult", "lastTestResultTimestamp", "eservice",
							"userType" };
					XSSFWorkbook workBook = new XSSFWorkbook();
					PoiUtil.createSheet(workBook, header, totalEvaLogs);
					Sheet sheet = workBook.getSheetAt(0);
					Row sheetHeader = workBook.getSheetAt(0).getRow(0);
					for (int i = 0; i <= sheetHeader.getPhysicalNumberOfCells(); i++) {
						sheet.autoSizeColumn(i);
					}
					workBook.write(bos);

					InputStream inputDetail = new ByteArrayInputStream(bos.toByteArray());
					Blob blobDetail = Hibernate.createBlob(inputDetail);
					build.setDetailFile(blobDetail);
					build.setStatus(Status.DONE);
					build.update();

					bos.flush();
					bos.close();
				} catch (Exception e) {
					LOG.info(e.toString());
					build.setStatus(Status.FAILED);
					build.setStatusMsg(e.toString());
					build.update();
				} finally {
					try {
						Thread.sleep(1000);
					}
					catch (InterruptedException ignoreIt) {}
				}
			}
		}

		synchronized void warmUp(QAEvaluationLog build) {
			if (tenantWarmedUp.contains(build.getTenantId())) return;
			try {
				
				String url = WiseSystemConfig.get().getHostname()
						+ WiseSystemConfig.get().getContextPath()
						+ "/qa-ajax.jsp?q=test&id=1&testMode=true&tid=" 
						+ build.getTenantId();
				
				List<Apikey> apikeys = Apikey.list(build.getTenantId());
				if (apikeys.size() > 0) {
					url += "&apikey=" + apikeys.get(0).getApikey();
				}
				LOG.info("Initial tenant : " + url);
				Thread.sleep(30000);
				String respStr = WiSeUtils.getDataFromUrl(url);

				JSONObject r = new JSONObject(respStr);
				LOG.info("warmUp result : " + r);
				if (r.has("output")) {
					tenantWarmedUp.add(build.getTenantId());
					Thread.sleep(30000);
				}
			}
			catch (Exception e) {
				try {
					Thread.sleep(10000);
				}
				catch (Exception e1) {
				}
			}
		}
	}

	public class QaAjaxRunner implements Runnable {
		private String qaApiUrl;
		private EvaluationLogEntity log;
		private AtomicInteger[] stat;
		private Tenant t;
		private AtomicInteger counter;
		private AtomicInteger total;
		private int apiMark;
		private QAEvaluationLog build;
		private ExecutorService executor;

		public QaAjaxRunner(String qaApiUrl, int apiMark, Tenant t, EvaluationLogEntity log, AtomicInteger[] stat,
				AtomicInteger counter, AtomicInteger total, QAEvaluationLog build, ExecutorService executor) {
			this.qaApiUrl = qaApiUrl;
			this.apiMark = apiMark;
			this.t = t;
			this.log = log;
			this.stat = stat;
			this.counter = counter;
			this.total = total;
			this.build = build;
			this.executor = executor;
			stat[2].incrementAndGet(); // 先新增一筆 unknown，跑完有結果再減去，不然好像常常會有不見的
		}

		@Override
		public void run() {
			if (build.getStatus() == Status.CANCELED) {
				executor.shutdownNow();
				return;
			}
			if (killSignal)
				return;

			int currFlag = log.getFlag();

			try {
				String url = qaApiUrl + "?q=" + URLEncoder.encode(log.getQuestion(), "UTF-8") + "&testMode=true"
						+ "&ftc=false" + "&tid=" + t.getId() + "&html=false" + "&qaCategorySelect="
						+ URLEncoder.encode(log.getQaCategory(), "UTF-8");

				// 若有 APIKEYS，取第一個
				List<Apikey> apikeys = Apikey.list(t.getId());
				if (apikeys.size() > 0) {
					url += "&apikey=" + apikeys.get(0).getApikey();
				} else {
					LOG.info("Tenant [" + t.getName() + " (" + t.getId()
							+ ")] has no apikey, the auto evaluation job be killed now.");
					killSignal = true;
					return;
				}
				LOG.info("TestMode URL : " + url);
				String respStr = WiSeUtils.getDataFromUrl(url);

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

				if (ltr != null && totalEvaLogs.contains(log)) {
					int listIndex = totalEvaLogs.indexOf(log);
					log.setLastTestResult(ltr);
					log.setLastTestResultTimestamp(Calendar.getInstance().getTime());
					if (resp.has("currentQaCategory")) {
						log.setQaCategory(resp.getString("currentQaCategory"));
					}

					if (currKid != null) {
						log.setLastRobotAnswerId(currKid);
					}
					totalEvaLogs.set(listIndex, log);
				}
			} catch (Exception e) {
				LOG.info("Error during AutoEvaluationJob: EvaluationLogEntity ID [" + log.getId() + " / "
						+ log.getQuestion() + "]:" + e.getMessage());
			} finally {
				int val = counter.incrementAndGet();
				float percent = Math.round((val * 10000f) / total.get()) / 100;
				String statusMsg = "Progress " + apiMark + " (" + val + "/" + total.get() + ") " + percent
						+ "% elasped " + sw.elapsed(TimeUnit.SECONDS) + "s - " + new java.util.Date();
				build.setStatusMsg(statusMsg);
				build.setProgress(percent);
				build.update();

			}
		}
	}

}
