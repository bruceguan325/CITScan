package com.intumit.solr.robot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.solr.common.SolrDocument;
import org.elasticsearch.common.lang3.builder.EqualsBuilder;

import com.hazelcast.core.ITopic;
import com.intumit.hithot.HitHotLocale;
import com.intumit.message.MessageUtil;
import com.intumit.solr.NotificationEvent;
import com.intumit.solr.NotificationEvent.NotificationType;
import com.intumit.solr.NotificationEvent.StackType;
import com.intumit.solr.NotificationEvent.TargetType;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.servlet.HazelcastUtil;
import com.intumit.solr.tenant.Tenant;

public class QAAltBuildQueue {

	public static enum Status implements Serializable {
		
		PENDING("waite.treat"), 
		SLOWJOB_PENDING("waite.treat.slow"), 
		PROCESSING("processing"), 
		SLOWJOB_PROCESSING("processing.slow"), 
		DONE("already.treat"),
		FAILED("detail.faile"),
		CANCELED("cancle.job"),
		UNKNOWN("unknown.state"),
		;
		
		public final String text;
		
		Status(String text){
			this.text = text;
		}
		
	}
	
	public static final int SLOW_JOB_THRESHOLD = 100000;
	
	static QAAltBuildQueue slowBQ = new QAAltBuildQueue();
	static QAAltBuildQueue fastBQ = new QAAltBuildQueue();
	
	/**
	 * The document must be indexed before added into build queue.
	 * Bcz we need read the document from index for copying fields.
	 * 
	 * @param docId
	 * @param kid
	 * @param qAltTpls
	 */
	public static void add(Integer tenantId, String docId, long kid, List<String> qAltTpls, String userName){
		QAAltBuild build = new QAAltBuild();
		build.setTenantId(tenantId);
		build.setDocId(docId);
		build.setKid(kid);
		build.setUserName(userName);
		build.setQAltTpls(StringUtils.join(qAltTpls, "\n"));
		build.setExpandSynonyms(false);
		
		long estimateAltCount = qAltTpls == null ? 0 : QASaver.estimateAltCount(tenantId, qAltTpls, false);
		
		if (estimateAltCount >= SLOW_JOB_THRESHOLD) {
			if (estimateAltCount <= QAUtil.ALT_LIMIT_PER_QUESTION)
				slowBQ._add(build);
			else 
				slowBQ.failedDocIds.add(QABuildInfo.fromQAAltBuild(build));
		}
		else {
			fastBQ._add(build);
		}
	}

	public static void startProcess() {
		for (QAAltBuild build : QAAltBuild.list()) {
			add(build.getTenantId(), build.getDocId(), build.getKid(), QAUtil.parseMultiValue(build.getQAltTpls()), build.getUserName());
		}
		slowBQ._startProcess();
		fastBQ._startProcess();
	}
	
	public static void removePendingBuilds(Integer tenantId, String docId){
		slowBQ._removePendingBuilds(tenantId, docId);
		fastBQ._removePendingBuilds(tenantId, docId);
	}
	
	public static void cancelRunningBuilds(Integer tenantId, String docId){
		slowBQ._cancelRunningBuildsBuilds(tenantId, docId);
		fastBQ._cancelRunningBuildsBuilds(tenantId, docId);
	}

	public static Status getStatus(Integer tenantId, String docId) {
		Status s = fastBQ._getStatus(tenantId, docId);
		
		if (s == Status.UNKNOWN) {
			s = slowBQ._getStatus(tenantId, docId);
			
			if (s == Status.PROCESSING) {
				s = Status.SLOWJOB_PROCESSING;
			}
			else if (s == Status.PENDING) {
				s = Status.SLOWJOB_PENDING;
			}
		}

		if (s == Status.UNKNOWN) {
			s = Status.DONE;
		}
		
		return s;
	}
	
	//
	// Non static area
	//
	private final LinkedBlockingQueue<QAAltBuild> queue = new LinkedBlockingQueue<QAAltBuild>();
	
	private final Set<MyRunner> processingRunners = new CopyOnWriteArraySet<MyRunner>();

	private final Set<QABuildInfo> canceledDocIds = new CopyOnWriteArraySet<QABuildInfo>();
	private final Set<QABuildInfo> failedDocIds = new CopyOnWriteArraySet<QABuildInfo>();
	
	private final ExecutorService runner = Executors.newSingleThreadExecutor();
	
	void _add(QAAltBuild build) {
		synchronized (queue) {
			removePendingBuilds(build.getTenantId(), build.getDocId());
			build.deleteByDocIdAndSave(); // 先清空並且儲存完再放到 queue 當中避免 queue 太快被 process 產生 racing condition
			if (queue.add(build)) {
				// 成功加入 queue 就重設下列狀態
				failedDocIds.remove(QABuildInfo.fromQAAltBuild(build));
				canceledDocIds.remove(QABuildInfo.fromQAAltBuild(build));
			}
		}
	}
	
	Status _getStatus(Integer tenantId, String docId) {
		Status s = Status.UNKNOWN;
		boolean processing = false;
		
		for (MyRunner r: processingRunners) {
			if (StringUtils.equalsIgnoreCase(r.saver.getBuild().getDocId(), docId) && tenantId.equals(r.saver.getBuild().getTenantId())) {
				processing = true;
				break;
			}
		}
		if (processing){
			s = Status.PROCESSING;
		}
		else {
			for(QAAltBuild b : queue){
				if (b.getDocId().equals(docId) && tenantId.equals(b.getTenantId())){
					s = Status.PENDING;
					break;
				}
			}
			
			// 若在失敗清單中但也還在 queue 裡頭，則不顯示失敗改顯示待處理
			if (s != Status.PENDING && failedDocIds.contains(new QABuildInfo(tenantId, docId))) {
				s = Status.FAILED;
			}
			if (s != Status.PENDING && canceledDocIds.contains(new QABuildInfo(tenantId, docId))) {
				s = Status.CANCELED;
			}
		}
		return s;
	}

	void _startProcess(){
		runner.execute(new MyRunner());
	}
	
	int getProcessingCount() {
		return processingRunners.size();
	}
	
	Collection<QABuildInfo> getProcessingBuildInfos() {
		Set<QABuildInfo> buildInfos = new HashSet<QABuildInfo>();
		
		for (MyRunner r: processingRunners) {
			buildInfos.add(QABuildInfo.fromQAAltBuild(r.saver.getBuild()));
		}
		return buildInfos;
	}
	
	int getPendingCount() {
		return queue.size();
	}
	
	Collection<QABuildInfo> getPendingBuildInfos() {
		List<QABuildInfo> buildInfos = new ArrayList<QABuildInfo>();

		for (QAAltBuild b : queue){
			buildInfos.add(QABuildInfo.fromQAAltBuild(b));
		}
		return buildInfos;
	}
	
	int getFailedCount() {
		return failedDocIds.size();
	}
	
	Collection<String> getFailedIds() {
		return Arrays.asList(failedDocIds.toArray(new String[0]));
	}
	
	void _removePendingBuilds(Integer tenantId, String docId){
		Set<QAAltBuild> toRemoves = new HashSet<QAAltBuild>();
		for(QAAltBuild b : queue){
			if (b.getDocId().equals(docId) && tenantId.equals(b.getTenantId())){
				toRemoves.add(b);
			}
		}
		queue.removeAll(toRemoves);
	}

	private void _cancelRunningBuildsBuilds(Integer tenantId, String docId) {
		for (MyRunner r: processingRunners) {
			if (StringUtils.equalsIgnoreCase(r.saver.getBuild().getDocId(), docId) && tenantId.equals(r.saver.getBuild().getTenantId())) {
				r.cancel();
			}
		}
	}
	
	public class MyRunner implements Runnable {
		QASaver saver = null;
		
		public MyRunner() {
			super();
		}

		public void cancel() {
			if (saver != null)
				saver.cancel();
		}

		@Override
		public void run() {
			while(true){
				try {
					QAAltBuild build = queue.take();
					Tenant t = Tenant.get(build.getTenantId());
					
					try {
						QA doc = null;
						SolrDocument mainDoc = null;
						QAUtil qu = QAUtil.getInstance(t);
						
						if (build.getDocId().contains(QAUtil.DATATYPE_COMMON_SENSE)) {
							mainDoc = qu.getMainQASolrDocument(build.getKid(), true);
						}
						
						if (t.getEnableQaAudit() && build.getDocId().contains(QAUtil.DATATYPE_QA_AUDIT))
							mainDoc = qu.getFilterQASolrDocument(build.getKid(), true, QAUtil.DATATYPE_QA_AUDIT, false);
						
						if (mainDoc != null) {
							doc = new QA(mainDoc);
						}
						else {
							doc = new QA();
						}
						
						doc.setId(build.getDocId());
						doc.setKid(build.getKid());
						doc.setUpdateInfo(new Date(), AdminUserFacade.getInstance().getByLoginName(build.getUserName()));

						Locale locale = Locale.TAIWAN;
						
						if (t.getLocale() == HitHotLocale.ja_JP) {
							locale = Locale.JAPANESE;
						}
						
						// 標準問不只直接放入問句，也要斷詞後放入問法
						String questionNlp = QAUtil.nlp(t, qu, doc.getQuestion(), locale, ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, TemperalAndNumberStrategy.PREBUILD_STRATEGY_DONT_CHANGE_ANYTHING).getSegResult();
						saver = new QASaver(build, doc, questionNlp);
						
						processingRunners.add(this);
						saver.save();
						
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e1) {
						}
						failedDocIds.remove(QABuildInfo.fromQAAltBuild(build));	// 無論如何只要有成功就從失敗清單中移除
						
						if (QAUtil.NO_MORE_EXPENSIVE_SYN_EXPAND || build.isExpandSynonyms()) {
							build.delete();				// 成功才從資料庫刪除 QAAltBuild
							// 發送成功通知
							doNotification(build, Status.DONE);

						}
						else {
							build.setExpandSynonyms(true);
							slowBQ._add(build);
							// 發送成功通知
							doNotification(build, Status.SLOWJOB_PENDING);
						}
					} catch (CanceledException canceld) {
						build.delete();				// 取消的也刪除排程
						doNotification(build, Status.CANCELED);
						canceledDocIds.add(QABuildInfo.fromQAAltBuild(build));
					} catch (Exception e2) {
						e2.printStackTrace();		/** @TODO 也許之後要考慮紀錄 exception **/
						failedDocIds.add(QABuildInfo.fromQAAltBuild(build));
					} finally {
						processingRunners.remove(this);	// 有 exception 也要移除不然會有卡住的狀態
					}
				} catch (Exception e) {
					// queue 有錯誤就暫時不管
					e.printStackTrace();
				}
			}
		}
	}
	
	void doNotification(QAAltBuild build, Status status) {
		try {
			NotificationEvent e = new NotificationEvent();
			e.setSource(QAAltBuildQueue.class.getName());
			e.setTargetType(TargetType.TENANT);
			e.setTarget(build.getTenantId());
			e.setStackType(StackType.STACK);
			e.setTitle(MessageUtil.getMessage(Locale.getDefault(), "notify.qa.altBuild.status.change"));
			e.setContent(MessageUtil.getMessage(Locale.getDefault(), "knowledge.num")+"[" + build.getKid() + "]：" + MessageUtil.getMessage(Locale.getDefault(), status.text));

			if (status == Status.DONE) {
				e.setNotificationType(NotificationType.SUCCESS);
			}
			else if (status == Status.FAILED) {
				e.setNotificationType(NotificationType.ERROR);
			}
			else {
				e.setNotificationType(NotificationType.INFO);
			}

			ITopic topic = HazelcastUtil.getTopic( "system-notification" );
			topic.publish(e);
		}
		catch (Exception e) {
			HazelcastUtil.log().error("Cannot publish notification message", e);
		}
	}
	
	static class QABuildInfo {
		Integer tenantId;
		String docId;
		
		public QABuildInfo(Integer tenantId, String docId) {
			this.tenantId = tenantId;
			this.docId = docId;
		}
		
		static QABuildInfo fromQAAltBuild(QAAltBuild build) {
			return new QABuildInfo(build.getTenantId(), build.getDocId());
		}
		
		@Override
		public int hashCode() {
			return new HashCodeBuilder().append(tenantId).append(docId).toHashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof QABuildInfo) {
				QABuildInfo that = (QABuildInfo)o;
				return new EqualsBuilder().append(this.tenantId, that.tenantId).append(this.docId, that.docId).isEquals();
			}
			return false;
		}
	}
}
