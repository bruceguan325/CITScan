package com.intumit.solr.robot.qarule;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.ANSWER_TYPE;
import com.intumit.solr.robot.QADialogConfig;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.ReconstructQuestionParameter;
import com.intumit.solr.robot.qadialog.QADialog;
import com.intumit.solr.robot.qadialog.Trigger;
import com.intumit.solr.tenant.Tenant;

/**
 * 檢查是否是 Dialog
 * 
 * @author herb
 */
public class QADialogRule extends QAMatchingRule {
	public static Map<Integer, Map<String, List<Trigger>>> dialogTriggerMap = new HashMap<Integer, Map<String, List<Trigger>>>();
	
	boolean allowMultipleDialogs = false;
	long maxConcurrentDialogs = 3;

	public QADialogRule(Map<String, Object> configs) {
		super();
		init(configs);
	}
	
	@Override
	public void init(Map<String, Object> configs) {
		if (configs.containsKey("maxConcurrentDialogs")) {
			maxConcurrentDialogs = (Long)configs.get("maxConcurrentDialogs");
		}
		if (configs.containsKey("allowMultipleDialogs")) {
			allowMultipleDialogs = (Boolean)configs.get("allowMultipleDialogs");
		}
		
		for (Tenant t: Tenant.list()) {
			reloadDialogConfig(t.getId());
		}
	}
	
	public static void reloadDialogConfig(Integer tenantId) {
		Map<String, List<Trigger>> triggersMap = dialogTriggerMap.get(tenantId);
		
		try {
			// 這裡是判斷設定檔有沒有改（利用 lastModified 作為判讀），有改就 reload
			// 理想上這裡後續應該改成存到 tenant 當中？（不然 load balance 很難做）
			List<QADialogConfig> dialogs = QADialogConfig.list(tenantId);
			triggersMap = new HashMap<String, List<Trigger>>();

			for (QADialogConfig dialog: dialogs) {
				JSONObject cfg = dialog.getDialogConfigObject();
				String mkey = dialog.getMkey();
				
				if (cfg.has("enterTrigger")) {
					JSONArray enterTriggerJsons = cfg.getJSONArray("enterTrigger");
					List<Trigger> triggers = new ArrayList<Trigger>();
					
					for (int j=0; j < enterTriggerJsons.length(); j++) {
						JSONObject trigger = enterTriggerJsons.getJSONObject(j);
						Trigger t = Trigger.createTrigger(trigger);
						
						if (t != null)
							triggers.add(t);
					}
					
					triggersMap.put(mkey, triggers);
				}
			}
			dialogTriggerMap.put(tenantId, triggersMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}
	
	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		if ("####".equals(ctx.getOriginalQuestion())) {
			List<QADialog> dialogs = new ArrayList<>();
			ctx.setCtxAttr("dialogs", dialogs);
			ctx.setAnswerText("FORCE QUIT ALL DIALOGS");
			ctx.setAnswerType(ANSWER_TYPE.DIALOG);
			
			return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
		}
		
		String NL = ctx.isClientSupportHtml() ? "<BR>" : "\n";
		List<QADialog> dialogs = (List<QADialog>)ctx.getCtxAttr("dialogs"); // 目前已經進行中的 dialog
		Map<String, List<Trigger>> triggersMap = dialogTriggerMap.get(ctx.getTenant().getId());
		if (triggersMap == null) triggersMap = new HashMap<String, List<Trigger>>();
		
		QADialog firstDialogInstance = null;
		List<String> runningDialogMkeys = new ArrayList<String>();
		
		// 有哪些已經有 instance 的 Dialog
		if (dialogs != null && dialogs.size() > 0) {
			Iterator<QADialog> itr = dialogs.iterator();
			while (itr.hasNext()) {
				QADialog dlg = itr.next();
				if (dlg == null) {
					itr.remove();
					continue;
				}
				
				if (firstDialogInstance == null) firstDialogInstance = dlg;
				runningDialogMkeys.add(dlg.getMkey());
			}
		}
		
		if (ANSWER_TYPE.DIALOG_SWITCH == ctx.getLastAnswerType()) {
			String mkey = (String)ctx.getCtxAttr("goingToSwitchToDialog");
			String question = (String)ctx.getCtxAttr("goingToSwitchToDialogQuestion");
			
			try {
				if (QAUtil.isConfirmWithYes(ctx.getCurrentQuestion())) {
					firstDialogInstance.deactivate();
					QADialog toDlg = QADialogRule.getDialogInstance(ctx, mkey);
					toDlg.setHasBeenDoubleConfirmed(true);
					
					if (!allowMultipleDialogs) {
						dialogs.clear();
					}
					dialogs.add(0, toDlg);
					firstDialogInstance = toDlg;
					ctx.setCurrentQuestion(question);
					
					// 設定回舊的問題，alts 也得再設定一次
					List<String> alts = QAUtil.reconstructQuestion(question, ReconstructQuestionParameter.DEFAULT_USER_INPUT_PARAM, ctx, QAUtil.getInstance(ctx.getTenant()).getToAnalysis());
					if (!alts.contains(question))
						alts.add(0, question);
					
					ctx.setRequestAttribute(QAContext.REQ_ATTR_RECONSTRUCTED_QUESTION, alts);
					
					toDlg.activate();
				}
				else if (QAUtil.isConfirmWithNo(ctx.getCurrentQuestion())) {
					ctx.setAnswerText("{{F:DIALOG_CONTINUE}}");
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (allowMultipleDialogs) {
			triggerLoop: 
			for (Map.Entry<String, List<Trigger>> entry: triggersMap.entrySet()) {
				String mkey = entry.getKey();
				
				// 第一個就是 active 的，不應該再 trigger
				if (firstDialogInstance != null && firstDialogInstance.getMkey().equals(mkey))
					continue;
				
				for (Trigger t: entry.getValue()) {
					if (t.isTrigger(ctx)) {
						try {
							JSONObject cfg = QADialogConfig.getByKey(ctx.getTenant().getId(), mkey).getDialogConfigObject();
							if (runningDialogMkeys.size() > 0) {
								// 如果是已經 running dialog，問要不要切回去
								if (runningDialogMkeys.contains(mkey)) {
									QADialog toDlg = findRunningDialog(ctx, mkey);
									// toDlg shouldn't be null
									ctx.setAnswerText("{{F:DIALOG_ASK_RETURN_TEXT1}}" + toDlg.getDescription() + "{{F:DIALOG_ASK_RETURN_TEXT2}}");
									ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG_SWITCH);
									ctx.setCtxAttr("goingToSwitchToDialog", mkey);
									ctx.setCtxAttr("goingToSwitchToDialogQuestion", ctx.getCurrentQuestion());

									return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
								}
								else {
									String desc = cfg.getString("dialogDesc");
									ctx.setAnswerText("{{F:DIALOG_SWITCH_TEXT1}}" + firstDialogInstance.getDescription() + "{{F:DIALOG_SWITCH_TEXT2}}" + desc + "{{F:DIALOG_SWITCH_TEXT3}}");
									ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG_SWITCH);
									ctx.setCtxAttr("goingToSwitchToDialog", mkey);
									ctx.setCtxAttr("goingToSwitchToDialogQuestion", ctx.getCurrentQuestion());
	
									return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
								}
							}
							else {
								String clazzName = cfg.getString("class");
								System.out.println("Create QADialog Instance[" + clazzName + "].");
								firstDialogInstance = (QADialog)Class.forName(clazzName).newInstance();
								firstDialogInstance.init(cfg);
								firstDialogInstance.activate();
								
								if (dialogs == null) {
									dialogs = new ArrayList<>();
									ctx.setCtxAttr("dialogs", dialogs);
								}
								dialogs.add(0, firstDialogInstance);
			
								break triggerLoop; // 第一個 trigger 就跳出，意思是排越前面的 trigger，以及排越前面的 dialog 會優先進入
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
			
			if (firstDialogInstance != null) {
				PostRuleCheckResult r = firstDialogInstance.check(ctx);
				
				if (!firstDialogInstance.isActive()) {
					if (dialogs.remove(firstDialogInstance)) {
						// 垃圾桶是用來準備回收用，但有時候在最後 QAContext.genResponseJSON() 會需要取用 dlg 的資料，不能這麼快刪除
						List<QADialog> trashCan = (List<QADialog>)ctx.getRequestAttribute("_dlgTrashCan");
						if (trashCan == null) {
							trashCan = new ArrayList<QADialog>();
							ctx.setRequestAttribute("_dlgTrashCan", trashCan);
						}
						trashCan.add(firstDialogInstance);
					}
				
					if (dialogs.size() > 0) {
						firstDialogInstance = dialogs.get(0);
						firstDialogInstance.activate();
						
						ctx.appendAnswerText(NL + "{{F:DIALOG_RETURNED_TEXT1}}" + firstDialogInstance.getDescription() + "{{F:DIALOG_RETURNED_TEXT2}}");
						
						if (firstDialogInstance.getInstructionText() != null) {
							ctx.appendAnswerText(NL + firstDialogInstance.getInstructionText());
						}
					}
				}

				if (r != PostRuleCheckResult.DEFAULT_CONTINUE_RESULT)
					return r;
			}
		}
		else {
			// 如果進行中的 dialog，就以進行中的為主，意思是同時間只能有一個因為 enterTrigger 啟動的 dialog
			if (dialogs != null && dialogs.size() > 0) {
				Iterator<QADialog> itr = dialogs.iterator();
				
				while (itr.hasNext()) {
					QADialog dlg = itr.next();
					PostRuleCheckResult r = dlg.check(ctx);
					
					if (!dlg.isActive()) {
						itr.remove();
						// 垃圾桶是用來準備回收用，但有時候在最後 QAContext.genResponseJSON() 會需要取用 dlg 的資料，不能這麼快刪除
						List<QADialog> trashCan = (List<QADialog>)ctx.getRequestAttribute("_dlgTrashCan");
						if (trashCan == null) {
							trashCan = new ArrayList<QADialog>();
							ctx.setRequestAttribute("_dlgTrashCan", trashCan);
						}
						trashCan.add(dlg);
					}
					
					if (r != PostRuleCheckResult.DEFAULT_CONTINUE_RESULT)
						return r;
				}
			}
			
			if (!ctx.hasAnswerText()) {
				// 如果沒有進行中的或者進行中的Dialog沒有結果，就看有沒有其他符合 enterTrigger
				triggerLoop: 
				for (Map.Entry<String, List<Trigger>> entry: triggersMap.entrySet()) {
					String mkey = entry.getKey();
					
					for (Trigger t: entry.getValue()) {
						if (t.isTrigger(ctx)) {
							try {
								JSONObject cfg = QADialogConfig.getByKey(ctx.getTenant().getId(), mkey).getDialogConfigObject();
								String clazzName = cfg.optString("class", "com.intumit.solr.robot.qadialog.QAConversationalDialog");
								System.out.println("Create QADialog Instance[" + clazzName + "].");
								QADialog ffd = (QADialog)Class.forName(clazzName).newInstance();
								ffd.init(cfg);
								ffd.activate();
								
								if (dialogs == null) {
									dialogs = new ArrayList<>();
									ctx.setCtxAttr("dialogs", dialogs);
								}
								dialogs.clear();	// 因為這個大 block 是屬於「allowMultipleDialogs = false」區，因此無論如何都先清空正在進行的 dialogs。
								dialogs.add(ffd);
			
								break triggerLoop; // 第一個 trigger 就跳出，意思是排越前面的 trigger，以及排越前面的 dialog 會優先進入
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
				
				// 前面如果有 trigger，這裡 size() > 0，跑 check
				if (dialogs != null && dialogs.size() > 0) {
					Iterator<QADialog> itr = dialogs.iterator();
					
					while (itr.hasNext()) {
						QADialog dlg = itr.next();
						PostRuleCheckResult r = dlg.check(ctx);

						if (!dlg.isActive())
							itr.remove();

						if (r != PostRuleCheckResult.DEFAULT_CONTINUE_RESULT)
							return r;
					}
				}
			}
		}
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}
	
	public static QADialog findRunningDialog(QAContext ctx, String mkey) {
		List<QADialog> dialogs = (List<QADialog>)ctx.getCtxAttr("dialogs"); // 目前已經進行中的 dialog
		
		// 有哪些已經有 instance 的 Dialog
		if (dialogs != null && dialogs.size() > 0) {
			for (QADialog dlg: dialogs) {
				if (dlg.getMkey().equals(mkey))
					return dlg;
			}
		}
		
		dialogs = (List<QADialog>)ctx.getRequestAttribute("_dlgTrashCan"); // 目前已經進行中的 dialog
		
		// 有沒有本回合跑完即將被丟棄的 dialog
		if (dialogs != null && dialogs.size() > 0) {
			for (QADialog dlg: dialogs) {
				if (dlg.getMkey().equals(mkey))
					return dlg;
			}
		}
		
		return null;
	}
	
	/*public static void removeFromRunningDialogList(QAContext ctx, QADialog dlg) {
		List<QADialog> dialogs = (List<QADialog>)ctx.getCtxAttr("dialogs"); // 目前已經進行中的 dialog
		
		// 有哪些已經有 instance 的 Dialog
		if (dialogs != null && dialogs.size() > 0) {
			dialogs.remove(dlg);
		}
	}*/

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx, PostRuleCheckResult result) {
		return super.onPostRuleCheck(ctx, result);
	}

	/**
	 * 生一個 QADialog instance 出來，是靠 name 來作為 key 值
	 * 
	 * @param ctx
	 * @param mkey 就是 json 裡頭的 name 
	 * @return
	 */
	public static QADialog getDialogInstance(QAContext ctx, String mkey) {
		return getDialogInstance(ctx, mkey, false);
	}
	
	public static QADialog getDialogInstance(QAContext ctx, String mkey, boolean useDraft) {
		QADialogConfig dlgCfg = QADialogConfig.getByKey(ctx.getTenant().getId(), mkey);
		
		if (dlgCfg != null) {
			try {
				JSONObject cfg = null;
				if (useDraft && dlgCfg.hasDraft()) {
					cfg = dlgCfg.getDraftDialogConfigObject();
				}
				else {
					cfg = dlgCfg.getDialogConfigObject();
				}
				String clazzName = cfg.optString("class", "com.intumit.solr.robot.qadialog.QAConversationalDialog");
				System.out.println("Create QADialog Instance[" + clazzName + "].");
				QADialog ffd = (QADialog)Class.forName(clazzName).newInstance();
				ffd.init(cfg);
				
				return ffd;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * 找 QADialog config 出來，是靠 mkey 來作為 key 值
	 * 
	 * @param ctx
	 * @param mkey 就是 json 裡頭的 mkey 
	 * @return
	 */
	public static JSONObject getDialogConfig(Tenant t, String mkey) {
		QADialogConfig dlgCfg = QADialogConfig.getByKey(t.getId(), mkey);
		
		if (dlgCfg != null) {
			try {
				JSONObject cfg = dlgCfg.getDialogConfigObject();
				
				return cfg;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/*public static void updateDialogConfig(Tenant t, String name, JSONObject dialogConfig) {
		int tenantId = t.getId();
		
		try {
			File theDialogJsonFile = new File(new File(WiSeEnv.getHomePath()), "dialogs/" + tenantId + ".json");
			JSONObject dialogsConfigJson = null;
			
			if (theDialogJsonFile.exists()) {
				String dialogsConfigStr = FileUtils.getContentsAsString(theDialogJsonFile, "UTF-8");
				
				if (StringUtils.isNotEmpty(dialogsConfigStr)) {
					dialogsConfigJson = new JSONObject(dialogsConfigStr);
					JSONArray dialogsConfig = dialogsConfigJson.getJSONArray("dialogConfig");
					int offset = -1;
					
					for (int i=0; i < dialogsConfig.length(); i++) {
						JSONObject cfg = dialogsConfig.getJSONObject(i);
						String dialogName = cfg.getString("name");
						
						if (StringUtils.equalsIgnoreCase(dialogName, name)) {
							offset = i;
						}
					}
					
					if (offset != -1) {
						dialogsConfig.set(offset, dialogConfig);
					}
					else {
						dialogsConfig.add(dialogConfig);
					}
				}
			}
			
			if (dialogsConfigJson == null) {
				dialogsConfigJson = new JSONObject();
				JSONArray arr = new JSONArray();
				dialogsConfigJson.put("dialogConfig", arr);
				arr.add(dialogConfig);
			}
			
			IOUtils.writeUTF8(new FileOutputStream(theDialogJsonFile), new org.json.JSONObject(dialogsConfigJson.toString()).toString(4));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/
}
