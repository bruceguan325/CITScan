package com.intumit.solr.robot;

import java.io.Serializable;
import java.util.List;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.intumit.solr.robot.dictionary.AttentionKeywordDictionary;
import com.intumit.solr.robot.dictionary.KnowledgePointDictionary;
import com.intumit.solr.robot.dictionary.OtherPossibleQuestionsBySearchDictionary;
import com.intumit.solr.robot.entity.QAEntityDictionary;
import com.intumit.solr.robot.intent.QAIntentDictionary;
import com.intumit.solr.servlet.HazelcastUtil;
import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;
import com.intumit.solr.util.WiSeSnapPuller;

/**
 * 提供一個簡單的地方管理一些數據更新的跨伺服器通知
 * 
 * @author herb
 *
 */
public class EventCenter {
	public static synchronized void init() {
		ITopic topic = HazelcastUtil.getTopic("general-event");
		topic.addMessageListener(new GeneralMessageListener());

		HazelcastUtil.log().debug("Hz EventCenter initialized");
	}

	/**
	 * 只用來作為 DB 更新的通知機制（多主機無法自動得知 DB 資料異動，又不可能每次去比對，因此這個用來通知）
	 * 若有其他用途可以討論，但沒討論前建議不要拿來做非 DB 更新通知以外的用途
	 * 
	 * @param source   最好是來源 class.getName()，比較好記名字
	 * @param tenantId 如果該類型的 event 需要的話就傳遞，一般來說應該都會需要
	 * @param name     事件名稱
	 * @param msg
	 */
	public static void fireEvent(String source, Integer tenantId, String name, String msg) {
		ITopic topic = HazelcastUtil.getTopic("general-event");
		topic.publish(new GeneralEvent(source, tenantId, name, msg));
		HazelcastUtil.log()
				.debug(String.format("Hz EventCenter fireEvent [%s, %d, %s, %s]", source, tenantId, name, msg));
	}

	public static class GeneralMessageListener implements MessageListener<GeneralEvent> {

		@Override
		public void onMessage(Message<GeneralEvent> msg) {
			GeneralEvent event = msg.getMessageObject();
			HazelcastUtil.log().debug(String.format("Hz EventCenter onMessage [%s, %d, %s, %s]", event.source,
					event.tenantId, event.name, event.msg));

			// 這裡就讓大家放一些常用的 DB 異動處理機制，這裡的 code 是每一台跑 SmartRobot 的伺服器都會執行（包含 fireEvent
			// 的伺服器本身也會）
			try {
				if (QADialogConfig.class.getName().equals(event.source)) {
					if ("reload".equals(event.name)) {
						com.intumit.solr.robot.qarule.QADialogRule.reloadDialogConfig(event.tenantId);
					}
				} else if (com.intumit.solr.tenant.Tenant.class.getName().equals(event.source)) {
					if ("reload".equals(event.name)) {
						com.intumit.solr.robot.qarule.QAMatchRuleController.clear("tenant:" + event.tenantId);
						com.intumit.solr.robot.QAUtil.getInstance(event.tenantId).forceRebuildUserDefiniedDictionary();
					}
				} else if (com.intumit.solr.robot.QAChannel.class.getName().equals(event.source)) {
					if ("reload".equals(event.name)) {
						for (QAChannel ch : QAChannel.list(event.tenantId)) {
							com.intumit.solr.robot.qarule.QAMatchRuleController.clear("channel:" + ch.getId());
						}
					}
				} else if (com.intumit.solr.robot.wivo.WiVoEntry.class.getName().equals(event.source)) {
					com.intumit.solr.robot.wivo.WiVoEntryDictionary.clear(event.tenantId);
					com.intumit.solr.robot.wivo.WiVoUtil.clear(event.tenantId);
				} else if (com.intumit.solr.robot.intent.QAIntent.class.getName().equals(event.source)) {
					if ("reload".equals(event.name)) {
						com.intumit.solr.robot.intent.QAIntentDictionary.clear(event.tenantId);
					}
				} else if (com.intumit.solr.robot.entity.QAEntity.class.getName().equals(event.source)) {
					if ("reload".equals(event.name)) {
						com.intumit.solr.robot.entity.QAEntityDictionary.clear(event.tenantId);
						// CustomData 會去 load QAEntity 了
						com.intumit.solr.robot.dictionary.CustomDataDictionary.clearCache(event.tenantId);
						com.intumit.solr.robot.dictionary.CustomDataDictionary.clear(event.tenantId);
					}
				} else if (com.intumit.solr.robot.QASaver.class.getName().equals(event.source)) {
					if ("processed".equals(event.name)) {
						QAUtil.getInstance(event.tenantId).kid2Qmap.clear();
						com.intumit.solr.robot.entity.QAEntityDictionary.clear(event.tenantId);
					}
				} else if (com.intumit.solr.robot.WiSeReplicationSwitch.class.getName().equals(event.source)) {
					if ("reload".equals(event.name)) {
						List<WiSeReplicationSwitch> master = WiSeReplicationSwitch.listNodes(null,
								WiSeReplicationSwitch.MASTER);
						// 換MASTER
						WiSeSnapPuller.setMasterHost(master.get(0).getHost());
						WiSeSnapPuller.setMasterPort(master.get(0).getPort());
						com.intumit.solr.ClusterMembershipListener.getInstance().checkAndSwitchMode();
					}
				} else if (com.intumit.solr.synonymKeywords.SynonymKeywordFacade.class.getName().equals(event.source)) {
					if ("reload".equals(event.name)) {
						SynonymKeywordFacade.getInstance().reset();
						KnowledgePointDictionary.clear(event.tenantId);
						OtherPossibleQuestionsBySearchDictionary.clear(event.tenantId);
						AttentionKeywordDictionary.clear(event.tenantId);
						QAIntentDictionary.clear(event.tenantId);
						QAEntityDictionary.clear(event.tenantId);
						QAUtil.cleanInstance(event.tenantId);
					}
				}
			} catch (Exception ex) {
				HazelcastUtil.log().error(String.format("Hz EventCenter Error onMessage [%s, %d, %s, %s]", event.source,
						event.tenantId, event.name, event.msg), ex);
			}
		}
	}

	static class GeneralEvent implements Serializable {

		public GeneralEvent() {
			super();
		}

		public GeneralEvent(String source, Integer tenantId, String name, String msg) {
			super();
			this.source = source;
			this.tenantId = tenantId;
			this.name = name;
			this.msg = msg;
		}

		public String source;
		public Integer tenantId;
		public String name;
		public String msg;
	}
}
