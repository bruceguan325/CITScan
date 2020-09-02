package com.intumit.solr.robot.qadialog;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.intumit.android.search.util.TaiwanAddressNormalizeUtil;
import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.ANSWER_TYPE;
import com.intumit.solr.robot.QAContext.MenuSelectionBehavior;
import com.intumit.solr.robot.QAContext.MenuView;
import com.intumit.solr.robot.QAContext.OptionAction;
import com.intumit.solr.robot.QAContext.OptionMenu;
import com.intumit.solr.robot.QAContextManager;
import com.intumit.solr.robot.QAOutputTemplate;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.UserClue;
import com.intumit.solr.robot.UserClueTag;
import com.intumit.solr.robot.connector.line.RichMessage;
import com.intumit.solr.robot.entity.QAEntity;
import com.intumit.solr.robot.function.FunctionUtil;
import com.intumit.solr.robot.intent.QAIntent;
import com.intumit.solr.robot.qaplugin.ParsedOption;
import com.intumit.solr.robot.qaplugin.QADialogPlugin;
import com.intumit.solr.robot.qarule.PostRuleCheckResult;
import com.intumit.solr.robot.qarule.QAMatchRuleController;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeUtils;

import flexjson.JSONDeserializer;
import groovy.lang.Binding;

public class QAConversationalDialog extends QADialog {	
	private static final long serialVersionUID = -3943917388209986448L;
	
	boolean initialized = false;
	DialogNode root = null;
	DialogNode lastNode = null;
	DialogNode currentNode = null;
	Map<String, DialogNode> nodeMap = new HashMap<>();
	List<DialogNode> globalPrependNodes = new ArrayList<>();
	List<DialogNode> globalAppendNodes = new ArrayList<>();
	JSONArray firstLvNodes = null;
	Long entryPoint = null;
	
	/**
	 * Dialog scope memory, clear after Dialog deactivate.
	 */
	Map<String, Object> memory = new HashMap<>();
	
	DialogLogEntry currentLog = null;
	List<DialogLogEntry> logs = new ArrayList<>();
	List<Long> nodeStateChangeStack = null;
	
	int changeNodeStateCounter = 0;
	

	InnerStatus status = InnerStatus.INACTIVE;

	public static enum InnerStatus {
		INACTIVE,
		ACTIVE, 
		JUST_ARRIVED,
	}
	
	@Override
	public void loadConfig() {
		super.loadConfig();
	}
	
	synchronized void init(QAContext ctx) {
		if (!initialized) {
			root = new DialogNode();
			root.setId(null);
			root.setName("ROOT");
			
    		try {
    			firstLvNodes = config.optJSONArray("children");
    		}
    		catch (JSONException e) {
    			e.printStackTrace();
    		}
    
    		String ep = config.optString("entryPoint");
    		if (StringUtils.trimToNull(ep) != null) {
    			try {
    				entryPoint = new Long(ep);
    			}
    			catch (NumberFormatException ignore) {}
    		}
    
    		//String NL = ctx.isClientSupportHtml() ? "<br>" : "\n";
    		BuildMenuData md = new BuildMenuData(new HashSet<String>());
    		
    		for (int i=0; i < firstLvNodes.length(); i++) {
				try {
					firstLvNodes = config.optJSONArray("children");
					DialogNode m = buildDialogNode(ctx, firstLvNodes.getJSONObject(i), entryPoint, "QA-DLG-" + mkey, root, md);
				}
				catch (JSONException e) {
					e.printStackTrace();
				}
    		}
    
    		if (md.entryNode != null) {
    			currentNode = md.entryNode;
    		}
    		else {
    			currentNode = root;
    		}
    		
    		System.out.println(root.printNodeTree());
    		initialized = true;
		}
	}

	@Override
	public PostRuleCheckResult innerCheck(QAContext ctx) {
		ctx.setCtxAttr("activeDialog", this);
		currentLog = new DialogLogEntry();
		changeNodeStateCounter = 0;
		nodeStateChangeStack = new ArrayList<Long>();
		logs.add(currentLog);
		boolean isDebug = ctx.getTenant().getEnableDebug();
		
		if (!initialized) {
			init(ctx);
			
			if (isDebug) currentLog.appendLog("Dialog[" + name + "] initalized.");
		}
		currentLog.setFromNode(currentNode);
		PostRuleCheckResult pres = null;

		try {
    		if (callStack.size() > 0) {
    			CallStackData stackData = callStack.peek();
    			PostRuleCheckResult r = stackData.to.check(ctx);
    			if (!stackData.to.isActive()) {
    				callStack.pop();
    
    				PostRuleCheckResult prcr = returnFromDialog(stackData, ctx, r);
    				
    				if (prcr == PostRuleCheckResult.DEFAULT_RETURN_RESULT)
    					return prcr;
    			}
    			else if (r == PostRuleCheckResult.DEFAULT_RETURN_RESULT) {
    				return r;
    			}
    		}
    		
    		if (currentNode.getNodeState() == NodeState.DEACTIVE) {
    			pres = changeNodeState(ctx, this, currentNode, NodeState.JUST_ACTIVATED);
    		}
    		else if (currentNode.getNodeState() == NodeState.WAIT_INPUT) {
    			if (currentNode.getNodeType() == NodeType.TRANSPARENT) {
    				// should not be here， a transparent node never become WAIT_INPUT state
    			}
    			pres = changeNodeState(ctx, this, currentNode, NodeState.GOT_INPUT);
    		}		
		}
		finally {
			if (ctx.getCtxAttr("activeDialog") == this)
				ctx.setCtxAttr("activeDialog", null);
		}
		
		return pres != null ? pres : ctx.hasAnswerText() ? PostRuleCheckResult.DEFAULT_RETURN_RESULT : PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public String showCurrentStatusText() {
		StringBuilder b = new StringBuilder();
		
		for (Field f: fields) {
			if (f.getCurrentValue() != null) {
				if (b.length() > 0) b.append("，");
			
				b.append(f.getShowName() + ":" + f.getCurrentValue());
			}
			else {
				if (f.getRequire() > 0) {
					if (b.length() > 0) b.append("，");
					
					b.append(f.getShowName() + ":尚未填入");
				}
			}
		}
		return b.toString();
	}

	@Override
	public String convertText(QAContext ctx, String text) {
		String tmp = text;
		
		if (tmp.indexOf("${") != -1) {
			for (Field f: fields) {
				if (f.getCurrentValue() != null)
					tmp = tmp.replaceAll("\\$\\{" + f.getName() + "\\}", f.getCurrentValue());
				else 
					tmp = tmp.replaceAll("\\$\\{" + f.getName() + "\\}", "");
			}
		}
		
		return replaceVariables(ctx, tmp);
	}

	@Override
	public void reset() {
		for (Field f: fields) {
			f.setCurrentValue(null);
			f.setCurrentResult(null);
		}
	}

	@Override
	public void activate() {
		status = InnerStatus.JUST_ARRIVED;
	}

	@Override
	public void deactivate() {
		status = InnerStatus.INACTIVE;
	}

	@Override
	public boolean isActive() {
		return status != InnerStatus.INACTIVE;
	}

	public Object getDlgAttr(String key) {
		return memory.get(key);
	}

	public Map<String, Object> getDlgAttr() {
		return memory;
	}

	public void setDlgAttr(String key, Object val) {
		memory.put(key, val);
	}
	
	private class BuildMenuData {
		Set<String> proccessed;
		DialogNode entryNode = null;
		public BuildMenuData(Set<String> proccessed) {
			super();
			this.proccessed = proccessed;
		}
	}

	private DialogNode buildDialogNode(QAContext ctx, 
			JSONObject data, Long entryPointId,
			String key, DialogNode parentNode, BuildMenuData menuData) {

		DialogNode n = new DialogNode();
		try {
			n.setNodeEnable(data.optBoolean("nodeEnable", true));
			
			n.setId(data.getLong("id"));
			n.setName(data.getString("text"));
			n.setScript(data.optString("script"));
			if (data.has("nodeType")) {
				n.setNodeType(NodeType.valueOf(StringUtils.defaultIfEmpty(data.getString("nodeType"), NodeType.NORMAL.name())));
			}
			n.setExtraParams(data.optJSONObject("extraParams"));
			n.setGlobalPrepend(data.optBoolean("globalPrepend", false));
			n.setGlobalAppend(data.optBoolean("globalAppend", false));
			n.setProtectMode(data.optBoolean("protectMode", false));
			n.setSpecialCheckMode(data.optBoolean("specialCheckMode", false));
			
			n.setGlobalNodePerceptionCheckPoint(NodeState.valueOf(data.optString("globalNodePerceptionCheckPoint", NodeState.GOT_INPUT.name())));
			
			
			if (data.has("perceptions")) {
				JSONArray arr = data.optJSONArray("perceptions");
				
				for (int i=0; i < arr.length(); i++) {
					JSONObject obj = arr.getJSONObject(i);
					Perception instant = (Perception) new JSONDeserializer().deserialize(obj.toString(), Perception.class);
					
					n.addPerception(instant);
				}
			}
			
			if (data.has("reactions")) {
				JSONArray arr = data.optJSONArray("reactions");
				
				for (int i=0; i < arr.length(); i++) {
					JSONObject obj = arr.getJSONObject(i);
					//System.out.println(obj.toString(2));
					Reaction instant = (Reaction) new JSONDeserializer().deserialize(obj.toString(), Reaction.class);
					
					n.addAction(instant);
				}
			}
			
			if (!n.getNodeEnable()) {
				// 被 disabled node 就到此為止
				return n;
			}
			
			if (parentNode != null) {
				n.setParentNode(parentNode);
				parentNode.addSubNode(n);
			}
			
			nodeMap.put(n.getId().toString(), n);
			
			if (entryPointId != null && entryPointId.equals(n.getId())) {
				menuData.entryNode = n;
			}
		
			if (n.getGlobalPrepend()) {
				globalPrependNodes.add(n);
			}
			if (n.getGlobalAppend()) {
				globalAppendNodes.add(n);
			}
			/*
			try {
				System.out.println("================");
				System.out.println(n.printNodeTree());
				System.out.println(new org.json.JSONObject(data.toString()).toString(2));
			}
			catch (org.json.JSONException e) {
				e.printStackTrace();
			}
			*/
			
			JSONArray children = data.optJSONArray("children");
			
			if (children != null && children.length() > 0) {
				for (int i=0; i < children.length(); i++) {
					DialogNode subNode = buildDialogNode(ctx, children.getJSONObject(i), entryPointId, key, n, menuData);
				}
			}
		}
		catch (JSONException e) {
			e.printStackTrace();
		}

		return n;
	}

	public static Map<String, String> getIdToQuestionMap(Tenant tenant, Set<String> hQaIds) {
		Map<String, String> rs = new HashMap<String, String>();
		if (CollectionUtils.isNotEmpty(hQaIds)) {
			String query = "";
			for (String id : hQaIds) {
				try {
					query += (StringUtils.isEmpty(query) ? "" : " OR ") + "id:\""
							+ URLEncoder.encode(id, "UTF-8") + "\"";
				}
				catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			SolrQuery q = new SolrQuery();
			q.setQuery(query);
			q.addFilterQuery("-isPart_i:[2 TO *]");
			q.addFilterQuery("dataType_s:" + QAUtil.DATATYPE_COMMON_SENSE);
			q.setRows(hQaIds.size());

			SolrServer server = tenant.getCoreServer();
			try {
				SolrDocumentList docs = server.query(q).getResults();
				for (SolrDocument doc : docs) {
					String id = (String) doc.getFieldValue("id");
					String ques = (String) doc.getFieldValue("QUESTION_s");
					rs.put(id, ques);
				}
			}
			catch (SolrServerException e) {
				e.printStackTrace();
			}
		}
		return rs;
	}
	
	PostRuleCheckResult changeNodeState(QAContext ctx, QAConversationalDialog dlg, DialogNode node, NodeState changeTo) {
		boolean isDebug = ctx.getTenant().getEnableDebug();
		changeNodeStateCounter++;
		nodeStateChangeStack.add(node.id);

		if (isDebug)
			currentLog.appendNL().appendLog("Dialog[" + name + "] changeNodeState (" + changeNodeStateCounter + ") [" + node.name + "] [" + node.nodeState + "->" + changeTo + "].");
		
		PostRuleCheckResult pres = null;
		node.setNodeState(changeTo);
		
		DialogNode triggeredNode = null;

		L1:
    	if (!node.getProtectMode()) {	
    		for (DialogNode o: globalPrependNodes) {
    			if (!o.isMe(node) && !o.isParentOf(node) && !nodeStateChangeStack.contains(o.id) && changeTo == o.globalNodePerceptionCheckPoint) {
        			for (Perception p: o.getPerceptions()) {
	    				if (p.getEnable() && p.check(ctx, this, node)) {
	    					currentLog.setPerceptBy(p);
	    					triggeredNode = o;
	    					
	    					if (isDebug) {
	    						currentLog.appendNL().appendLog("Dialog[" + name + "] triggered GPN [" + triggeredNode.getName() + "] (by perception: " + p.getLabel() + ")");
	    					}
	    					break L1; // 第一個觸發就跳出....
	    				}
        			}
    			}
    		}
		}
		
		if (changeTo == NodeState.GOT_INPUT) {
	    	if (triggeredNode == null) {
	    		L2:
	    		for (DialogNode o: currentNode.getSubNodes()) {
	    			for (Perception p: o.getPerceptions()) {
		    			if (p.getEnable() && p.check(ctx, this, o)) {
		    				currentLog.setPerceptBy(p);
		    				triggeredNode = o;
	    					
	    					if (isDebug) {
	    						currentLog.appendNL().appendLog("Dialog[" + name + "] triggered [" + triggeredNode.getName() + "] (by perception: " + p.getLabel() + ")");
	    					}
		    				break L2; // 第一個觸發就跳出....
		    			}
	    			}
	    		}
	    	}
	    		
	    	if (triggeredNode == null) {
	    		L3:
	    		if (!node.getProtectMode()) {	
    	    		for (DialogNode o: globalAppendNodes) {
    	    			if (!o.isMe(currentNode) && !o.isParentOf(currentNode) && !nodeStateChangeStack.contains(o.id)) {
    	        			for (Perception p: o.getPerceptions()) {
    		    				if (p.getEnable() && p.check(ctx, this, o)) {
    		    					currentLog.setPerceptBy(p);
    		    					triggeredNode = o;
    		    					
    		    					if (isDebug) {
    		    						currentLog.appendNL().appendLog("Dialog[" + name + "] triggered GPA [" + triggeredNode.getName() + "] (by perception: " + p.getLabel() + ")");
    		    					}
    		    					break L3; // 第一個觸發就跳出....
    		    				}
    	        			}
    	    			}
    	    		}
    	    	}
    		}
		}
        
		if (triggeredNode != null) {
			ctx.setRequestAttribute("_TRIGGERED_NODE", triggeredNode);
			pres = gotoNode(ctx, this, triggeredNode, null);
		}
		else {
    		// 如果腳本回傳不是 null，那麼就跳 reactions
    		Object scriptResult = null;
    		if (StringUtils.trimToNull(node.getScript()) != null) {
				Binding binding = new Binding();
				binding.setProperty("ctx", ctx);
				binding.setProperty("dlg", dlg);
				binding.setProperty("node", node);
    				
				try {
					scriptResult = GroovyUtil.runScript(binding, node.getScript());
				}
				catch (Exception ex) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ex.printStackTrace(pw);
					currentLog.appendNL().appendLog("Dialog[" + name + "] script error [" + currentNode.getName() + "]: " + sw.toString() + "");
					ex.printStackTrace();
				}
    		}
		
			if (scriptResult == null) {
        		for (Reaction r: node.getReactions()) {
        			if (r.getEnable() && r.getWhen() == changeTo) {
        				currentLog.addTriggeredReaction(r);
        				pres = r.doIt(ctx, dlg, node);
        				
        				if (pres != null && PostRuleCheckResult.DEFAULT_CONTINUE_RESULT != pres)
        					return pres;
        			}
        		}
    		}
    		else if (scriptResult instanceof PostRuleCheckResult) {
				if (isDebug) {
					currentLog.appendNL().appendLog("Dialog[" + name + "] run script of [" + node.getName() + "] => post rule check result [" + pres + "]");
				}
    			pres = (PostRuleCheckResult)scriptResult;
    		}
			
			if (node.getNodeType() != NodeType.TRANSPARENT) {
    			if (changeTo == NodeState.JUST_ACTIVATED) {
    				pres = changeNodeState(ctx, dlg, node, NodeState.WAIT_INPUT);
    			}
    			else if (changeTo == NodeState.GOT_INPUT) {
    				// 如果換到 GOT_INPUT，偏偏又沒有 triggeredNode，那就切回 WAIT_INPUT
    				pres = changeNodeState(ctx, dlg, node, NodeState.WAIT_INPUT);
    			}
			}
			else {
	    		if (changeTo == NodeState.JUST_ACTIVATED) {
	    			pres = changeNodeState(ctx, dlg, node, NodeState.GOT_INPUT);
				}
				else if (changeTo == NodeState.GOT_INPUT) {
					// NodeType.TRANSPARENT 不允許沒有 triggeredNode...
					throw new RuntimeException("Nothing triggered in transparent node, dialog got into dead end.");
				}
			}
		}
		
		return pres;
	}

	public DialogNode findDialogNode(String key) {
		return nodeMap.get(key);
	}
	
	public PostRuleCheckResult gotoNode(QAContext ctx, QAConversationalDialog dlg, DialogNode toNode, NodeState toState) {
		if (toState == null) {
			toState = NodeState.JUST_ACTIVATED;
		}
		boolean isDebug = ctx.getTenant().getEnableDebug();
		if (isDebug) currentLog.appendNL().appendLog("Dialog[" + name + "] switching [" + dlg.currentNode.name + "(" + dlg.currentNode.getNodeState() + ")] -> [" + toNode.name + "(" + toNode.getNodeState() + ")].");
		
		changeNodeState(ctx, dlg, currentNode, NodeState.LEAVING_NODE);
		lastNode = currentNode;
		currentNode = toNode;
		PostRuleCheckResult pres = changeNodeState(ctx, dlg, currentNode, toState);
		changeNodeState(ctx, dlg, lastNode, NodeState.DEACTIVE);
		dlg.currentLog.setToNode(toNode);
		if (isDebug) currentLog.appendNL().appendLog("Dialog[" + name + "] switched [" + dlg.lastNode.name + "(" + dlg.lastNode.getNodeState() + ")] -> [" + currentNode.name + "(" + currentNode.getNodeState() + ")].");
		
		return pres;
	}
	
	/**
	 * Extend OptionMenu by Delegating
	 * @author herb
	 *
	 */
	public static class DialogNode implements Serializable {
		Long id;
		String name;
		NodeType nodeType = null;
		NodeState nodeState = null;
		NodeState globalNodePerceptionCheckPoint = NodeState.GOT_INPUT;
		
		Boolean globalPrepend;
		Boolean globalAppend;
		Boolean protectMode;
		Boolean specialCheckMode;
		
		List<Perception> perceptions;
		List<Reaction> reactions;
		DialogNode parentNode = null;
		List<DialogNode> subNodes = null;
		String script;
		JSONObject extraParams = null;
		
		Boolean nodeEnable = null;
		
		public DialogNode() {
			super();
		}
		
		public boolean isMe(DialogNode currentNode) {
			if (getId() != null && currentNode.getId() != null) {
				return getId().equals(currentNode.getId());
			}
			
			return false;
		}

		public boolean isParentOf(DialogNode currentNode) {
			while (true) {
				DialogNode parent = currentNode.getParentNode();
				
				if (parent != null) {
					if (isMe(parent)) 
						return true;
					
					currentNode = parent;
				}
				else {
					break;
				}
			}
			return false;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public NodeType getNodeType() {
			return nodeType != null ? nodeType : NodeType.NORMAL;
		}

		public void setNodeType(NodeType nodeType) {
			this.nodeType = nodeType;
		}

		public NodeState getNodeState() {
			return nodeState != null ? nodeState : NodeState.DEACTIVE;
		}

		public void setNodeState(NodeState nodeStage) {
			this.nodeState = nodeStage;
		}

		public Boolean getGlobalPrepend() {
			return globalPrepend != null ? globalPrepend : false;
		}

		public void setGlobalPrepend(Boolean globalPrepend) {
			this.globalPrepend = globalPrepend;
		}

		public NodeState getGlobalNodePerceptionCheckPoint() {
			return globalNodePerceptionCheckPoint;
		}

		public void setGlobalNodePerceptionCheckPoint(NodeState globalNodePerceptionCheckPoint) {
			this.globalNodePerceptionCheckPoint = globalNodePerceptionCheckPoint;
		}

		public Boolean getSpecialCheckMode() {
			return specialCheckMode != null ? specialCheckMode : false;
		}

		public void setSpecialCheckMode(Boolean specialCheckMode) {
			this.specialCheckMode = specialCheckMode;
		}

		public Boolean getGlobalAppend() {
			return globalAppend != null ? globalAppend : false;
		}

		public void setGlobalAppend(Boolean globalAppend) {
			this.globalAppend = globalAppend;
		}

		public Boolean getProtectMode() {
			return protectMode != null ? protectMode : false;
		}

		public void setProtectMode(Boolean protectMode) {
			this.protectMode = protectMode;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Perception> getPerceptions() {
			if (perceptions == null) {
				perceptions = new ArrayList<Perception>();
			}
			return perceptions;
		}

		public void setPerceptions(List<Perception> perceptions) {
			this.perceptions = perceptions;
		}

		public void addPerception(Perception p) {
			if (perceptions == null) {
				perceptions = new ArrayList<Perception>();
			}
			perceptions.add(p);
		}

		public List<Reaction> getReactions() {
			if (reactions == null) {
				reactions = new ArrayList<Reaction>();
			}
			return reactions;
		}

		public void setReactions(List<Reaction> actions) {
			this.reactions = actions;
		}

		public void addAction(Reaction a) {
			if (reactions == null) {
				reactions = new ArrayList<Reaction>();
			}
			reactions.add(a);
		}

		public DialogNode getParentNode() {
			return parentNode;
		}

		public void setParentNode(DialogNode parentNode) {
			this.parentNode = parentNode;
		}

		public List<DialogNode> getSubNodes() {
			if (subNodes == null) {
				subNodes = new ArrayList<DialogNode>();
			}
			return subNodes;
		}

		public void setSubNodes(List<DialogNode> subNodes) {
			this.subNodes = subNodes;
		}
		
		public void addSubNode(DialogNode subNode) {
			if (subNodes == null) {
				subNodes = new ArrayList<DialogNode>();
			}
			this.subNodes.add(subNode);
		}

		public String getScript() {
			return script;
		}

		public void setScript(String script) {
			this.script = script;
		}

		public JSONObject getExtraParams() {
			return extraParams;
		}

		public void setExtraParams(JSONObject extraParams) {
			this.extraParams = extraParams;
		}

		public Boolean getNodeEnable() {
			return nodeEnable != null ? nodeEnable : true;
		}

		public void setNodeEnable(Boolean nodeEnable) {
			this.nodeEnable = nodeEnable;
		}

		@Override
		public String toString() {
			return "DialogNode [id=" + id + ", name=" + name + ", perceptions=" + perceptions + ", reactions=" + reactions
					+ ", globalPrepend=" + globalPrepend
					+ ", protectMode=" + protectMode
					+ ", parentNode=" + parentNode!=null?"notNull":"null" 
					+ ", subNodes=" + subNodes + ", script=" + script + ", extraParams=" + extraParams + "]";
		}
		
		public String printNodeTree() {
			StringBuffer buf = new StringBuffer();
			printNodeTree(buf, 0);
			return buf.toString();
		}
		void printNodeTree(StringBuffer buf, int lv) {
			String indent = "";
			for (int i=0; i < lv; i++)
				indent += "  ";
			buf.append(indent + "Node [id=" + id + ", name=" + name + ", " + nodeType + ", " + nodeState + ", GP? " + getGlobalPrepend() + ", Protect? " + getProtectMode() + "]\n");
			
			if (perceptions != null) {
				buf.append(indent + "    ");
				for (Perception a: perceptions) {
					buf.append(" [" + a.label + ":" + a.type +"]");
				}
				buf.append("\n");
			}
			
			if (reactions != null) {
				buf.append(indent + "    ");
				for (Reaction a: reactions) {
					buf.append(" (" + a.label + ":" + a.type +")");
				}
				buf.append("\n");
			}
			
			for (DialogNode n: getSubNodes()) {
				n.printNodeTree(buf, lv+1);
			}
		}
	}
	
	public static class Perception implements Serializable {
		String label;
		PerceptionType type;
		String criteria;
		String checkval;
		Boolean enable;
		public String getLabel() {
			return label;
		}
		public void setLabel(String label) {
			this.label = label;
		}
		public PerceptionType getType() {
			return type;
		}
		public void setType(PerceptionType type) {
			this.type = type;
		}
		public String getCheckval() {
			return checkval;
		}
		public void setCheckval(String checkval) {
			this.checkval = checkval;
		}
		public String getCriteria() {
			return criteria;
		}
		public void setCriteria(String criteria) {
			this.criteria = criteria;
		}
		public Boolean getEnable() {
			return enable != null ? enable : false;
		}
		public void setEnable(Boolean enable) {
			this.enable = enable;
		}
		
		public boolean check(QAContext ctx, QAConversationalDialog dlg, DialogNode node) {
			boolean result = false;
			
			if (checkval != null) {
				String val = FunctionUtil.collectExecAndReplace(checkval, ctx);
				String[] val1s = val.split(";");
				for (int i = 0; i < val1s.length; i++) {
					criteria = criteria.replace("VALREPLACE" + i, val1s[i]);
				}
			}
			
			criteria = StringUtils.trimToEmpty(criteria);
			
			switch (type) {
				case regex:
					Pattern p = Pattern.compile(criteria);
					Matcher m = p.matcher(ctx.getCurrentQuestion());
					List<String> gnames = WiSeUtils.getNamedGroupCandidates(criteria);
					
					if (m.find()) {
						result = true;
						
						if (gnames.size() > 0) {
							for (String name: gnames) {
								String val = m.group(name);
								
								if (val != null) {
									ctx.setRequestAttribute(name, val);
								}
							}
						}
					}
					else {
						List<String> alts = (List<String>)ctx.getRequestAttribute(QAContext.REQ_ATTR_RECONSTRUCTED_QUESTION);
						if (alts != null) {
							for (String alt: alts) {
								m = p.matcher(alt);
								if (m.find()) {
									result = true;
									
									if (gnames.size() > 0) {
										for (String name: gnames) {
											String val = m.group(name);
											
											if (val != null) {
												ctx.setRequestAttribute(name, val);
											}
										}
									}
									break;
								}
							}
						}
					}
					break;
				case confirm:
					if ("Y".equalsIgnoreCase(criteria)) {
						result = QAUtil.isConfirmWithYes(ctx.getCurrentQuestion());
					}
					else if ("N".equalsIgnoreCase(criteria)) {
						result = QAUtil.isConfirmWithNo(ctx.getCurrentQuestion());
					}
					break;
				case number_range:
					try {
						int index = criteria.indexOf("-");
						Integer number = Integer.parseInt(ctx.getCurrentQuestion());
						// 數字範圍(先用"-"來區分是否為數字範圍 (ex: 10000-50000)
						if (index > 0) {
							Integer num1 = Integer.parseInt(criteria.substring(0, index));
							Integer num2 = Integer.parseInt(criteria.substring(index + 1, criteria.length()));
							// 1小2大 (10000-50000)
							if (num1 < num2) {
								if (num1 <= number && num2 >= number) {
									result = true;
								} else {
									result = false;
								}
							// 1大2小 (50000-10000)
							} else {
								if (num2 <= number && num1 >= number) {
									result = true;
								} else {
									result = false;
								}
							}
						}
						// 一般數字
						else {
							if (number == Integer.parseInt(criteria)) {
								result = true;
							} else {
								result = false;
							}
						}
					} catch (Exception e) {
						result = false;
					}
					break;
				case string:
					if (criteria.equals(ctx.getCurrentQuestion())) {
						result = true;
					} else {
						result = false;
					}
					break;
				case direct_answer:
					SolrDocument directAnswer = ctx.getQAUtil().searchDirectAnswer(ctx.getCurrentQuestion(), ctx);
					if (directAnswer != null) {
						if (StringUtils.isNotEmpty(criteria)) {
							QA directQA = new QA(directAnswer);
							if (StringUtils.equals(criteria, "" + directQA.getKid())) {
								result = true;
							}
							else {
								result = false;
							}
						}
						else {
							result = true;
						}
					} 
					else {
						result = false;
					}
					break;
				case address:
					// 用數字當作條件範圍(1:只需行政區:縣/市, 2:承1並包含地區(市區鄉鎮), 3:承1.2並包含XX路 or XX街, 4:承1.2.3並包含號
					TaiwanAddressNormalizeUtil tanu = new TaiwanAddressNormalizeUtil(ctx.getCurrentQuestion());
					String[] addr = new String[] { tanu.getAdministrative_area(),
						tanu.getLocality(), tanu.getRoute(), tanu.getStreet_number()};
					try {
						Integer range = Integer.parseInt(criteria);
						if (range > addr.length && range < 1) {
							result = false;
						} else {
							for (int i = 0; i < addr.length; i++) {
								if (range >= (i + 1)) {
									if (addr[i] != null) {
										result = true;
									} else {
										result = false;
									}
								}
							}
						}
					} catch (Exception e) {
						result = false;
					}
					break;
				case date_time:
					/* 
					 * 用dateformat方式比對，但格式要完全一致。
					 * example :
					 * [criteria -> 'yyyy/MM/dd'], input:[2016/06/16 -> true, 20170616 -> false]
					 * [crtieria -> 'dd'], input:[1~31 -> true, > 0 or < 31 -> false]
					 * [crtieria -> 'hh'], input:[13 -> false]
					 */
					try {
						DateTimeFormatter formatter = DateTimeFormat.forPattern(criteria);
						DateTime date = formatter.parseDateTime(ctx.getCurrentQuestion());
						result = true;
					} catch (Exception e) {
						result = false;
					}
					break;
				case intent_entity:
					Pattern pp = Pattern.compile("(?is)([\\+\\-]?(@|#|%)[a-zA-Z_0-9]+)");
					Matcher mm = pp.matcher(criteria);
					Set<QAIntent> intents = (Set<QAIntent>)ctx.getRequestAttribute("intents");
					Set<QAEntity> entities = (Set<QAEntity>)ctx.getRequestAttribute("entities");
					
					boolean hasAtLeastOneShould = false;
					boolean allShouldAreFalse = true;
					
					while (mm.find()) {
						Operator op = null;
						String tmp = mm.group();
						if (tmp.startsWith("+")) {
							op = Operator.MUST;
							tmp = tmp.substring(1);
						}
						else if (tmp.startsWith("-")) {
							op = Operator.MUSTNOT;
							tmp = tmp.substring(1);
						}
						else {
							op = Operator.SHOULD;
						}
						
						String type = tmp.substring(0, 1);
						String name = tmp.substring(1);
						
						if (type.equals("@")) {
							switch (op) {
								case MUST:
									boolean mustCheck = false;
									for (QAIntent i: intents) {
										if (StringUtils.equalsIgnoreCase(name, i.getTag()))
											mustCheck = true;
									}
									if (!mustCheck) {
										return false;
									}
									else {
										result = true;
									}
									
									break;
								case MUSTNOT:
									for (QAIntent i: intents) {
										if (StringUtils.equalsIgnoreCase(name, i.getTag())) {
											return false;
										}
									}
									result = true;
									break;
								case SHOULD:
									hasAtLeastOneShould = true;
									for (QAIntent i: intents) {
										if (StringUtils.equalsIgnoreCase(name, i.getTag()))
											allShouldAreFalse = false;
									}
								default:
							}
						}
						
						if (type.equals("#")) {
							switch (op) {
								case MUST:
									boolean mustCheck = false;
									for (QAEntity i: entities) {
										if (StringUtils.equalsIgnoreCase(name, i.getCode()))
											mustCheck = true;
									}
									if (!mustCheck) {
										return false;
									}
									else {
										result = true;
									}
									
									break;
								case MUSTNOT:
									for (QAEntity i: entities) {
										if (StringUtils.equalsIgnoreCase(name, i.getCode())) {
											return false;
										}
									}
									result = true;
									break;
								case SHOULD:
									hasAtLeastOneShould = true;
									for (QAEntity i: entities) {
										if (StringUtils.equalsIgnoreCase(name, i.getCode()))
											allShouldAreFalse = false;
									}
								default:
							}
						}
						
						if (type.equals("%")) {
							
							String scope = "_CTX_";
							String name1 = name;
							if (StringUtils.startsWith(name, "_CTX_")) {
								scope = "_CTX_";
								name1 = name.substring(5);
							}
							else if (StringUtils.startsWith(name, "_DLG_")) {
								scope = "_DLG_";
								name1 = name.substring(5);
							}
							else if (StringUtils.startsWith(name, "_REQ_")) {
								scope = "_REQ_";
								name1 = name.substring(5);
							}
							else if (StringUtils.startsWith(name, "_REP_")) {
								scope = "_REP_";
								name1 = name.substring(5);
							}
							
							switch (op) {
								case MUST:
									boolean mustCheck = false;
									if ("_CTX_".equals(scope) && ctx.getCtxAttr(name1) != null) {
										mustCheck = true;
									}
									else if ("_DLG_".equals(scope) && dlg.memory.get(name1) != null) {
										mustCheck = true;
									}
									else if ("_REQ_".equals(scope) && ctx.getRequestAttribute(name1) != null) {
										mustCheck = true;
									}
									else if ("_REP_".equals(scope) && ctx.getResponseAttribute(name1) != null) {
										mustCheck = true;
									}
									if (!mustCheck) {
										return false;
									}
									else {
										result = true;
									}
									
									break;
								case MUSTNOT:
									if ("_CTX_".equals(scope) && ctx.getCtxAttr(name1) != null) {
										return false;
									}
									else if ("_DLG_".equals(scope) && dlg.memory.get(name1) != null) {
										return false;
									}
									else if ("_REQ_".equals(scope) && ctx.getRequestAttribute(name1) != null) {
										return false;
									}
									else if ("_REP_".equals(scope) && ctx.getResponseAttribute(name1) != null) {
										return false;
									}
									result = true;
									break;
								case SHOULD:
									hasAtLeastOneShould = true;
									if ("_CTX_".equals(scope) && ctx.getCtxAttr(name1) != null) {
										allShouldAreFalse = false;
									}
									else if ("_DLG_".equals(scope) && dlg.memory.get(name1) != null) {
										allShouldAreFalse = false;
									}
									else if ("_REQ_".equals(scope) && ctx.getRequestAttribute(name1) != null) {
										allShouldAreFalse = false;
									}
									else if ("_REP_".equals(scope) && ctx.getResponseAttribute(name1) != null) {
										allShouldAreFalse = false;
									}
								default:
							}
						}
						
						if (hasAtLeastOneShould) {
							if (allShouldAreFalse)
								result |= false;
							else
								result |= true;
						}
					}
					break;
				case unconditional:
					if (StringUtils.isNotEmpty(criteria)) {
						int maxTimes = Integer.parseInt(criteria);
						String nckey = "_Enter_Count_" + node.getId();
						
						if (dlg.memory.containsKey(nckey)) {
							if (((Number)dlg.memory.get(nckey)).intValue() >= maxTimes) {
								return false;
							}
							else {
								dlg.memory.put(nckey, ((Number)dlg.memory.get(nckey)).intValue() + 1);
							}
						}
						else {
							dlg.memory.put(nckey, 1);
						}
					}
					result = true;
					break;
				case identity_number:
					String question = ctx.getCurrentQuestion();
					if (question.matches("[a-zA-Z]\\d{9}") && question.length() == 10) {
						int[] letterNums = {10, 11, 12, 13, 14, 15, 16, 17,
								34, 18, 19, 20, 21, 22, 35, 23, 24, 25, 26,
								27, 28, 29, 32, 30, 31, 33};
						question = question.toUpperCase();
						char letter = question.charAt(0);
						question = letterNums[letter - 'A'] + question.substring(1);
						int total = question.charAt(0) - '0';
						for (int i = 0; i < 10; i++) {
							total += (question.charAt(i) - '0') * (10 - i);
						}
						int checkNum = (10 - total % 10) % 10;
						if (checkNum == (question.charAt(10) - '0')) {
							result = true;
						} else {
							result = false;
						}
					} else {
						result = false;
					}
					break;
				case inline_function:
					Object ifRes = FunctionUtil.collectAndExec(criteria, ctx);
					if (ifRes instanceof Boolean) {
						result = (Boolean)ifRes;
					}
					else if (ifRes instanceof String) {
						try {
							result = Boolean.parseBoolean((String)ifRes);
						}
						catch (Exception ignore) {
							result = false;
						}
					}
					break;
				case intent_entity_exit:
					result = false;
					question = ctx.getCurrentQuestion();
					pp = Pattern.compile("(@|#|%)(\\{\\{([a-zA-Z0-9_/\\s\\$\\+]+?)(:.+?)?\\}\\})");
					mm = pp.matcher(criteria);
					
					while (mm.find()) {
						String tmp = mm.group();						
						String type = tmp.substring(0, 1);
						String name = tmp.substring(1);
						
						if (type.equals("@")) {
							//check intent
						}
						
						if (type.equals("#")) {
							//check entity
							FunctionUtil.collectExecAndReplace(name, ctx);
							if(ctx.getRequestAttribute("QAENTITY") != null) {
								QAEntity entity = (QAEntity) ctx.getRequestAttribute("QAENTITY");
								pp = Pattern.compile(entity.getEntityValues());
								Matcher nn = pp.matcher(question);
								int i = 0;
								while (nn.find()) {
									if (i == 0) {
										ctx.setCurrentQuestion(nn.group());
									} else {
										ctx.setCurrentQuestion(ctx.getCurrentQuestion() + "-" + nn.group());
									}
									result = true;
									i++;
								}
							}
							
						}
						
						if (type.equals("%")) {
							//check _CTX_
						}
					}
					break;
				default:
			}
			
			return result;
		}
	}
	
	public static final List<String> EMPTY_LIST = new ArrayList<>();
	
	public static class Reaction implements Serializable {
		String label;
		NodeState when;
		ReactionType type;
		List<String> contents;
		String detail;
		Boolean enable;
		public String getLabel() {
			return label;
		}
		public PostRuleCheckResult doIt(QAContext ctx, QAConversationalDialog dlg, DialogNode node) {
			boolean isDebug = ctx.getTenant().getEnableDebug();

			Reaction r = this;
			
			if (isDebug)
				dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] do Reaction [" + r.label + " : " + r.getType() + "].");
			
			List<String> params = CollectionUtil.map(r.getContents(), new CollectionUtil.Mapper<String, String>() {
                @Override
                public String map(String item) {
                    return item.trim(); // trim all elements
                }
            });
			PostRuleCheckResult pres = null;
			String val = null;
    			
			switch (r.getType()) {
				case set_answer: 
					if (params.size() < 1) {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
					}
					else {
						ctx.setAnswerText((String)params.get(0));
						ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
						//pres = PostRuleCheckResult.DEFAULT_RETURN_RESULT;
					}
					break;
				case append_answer: 
					if (params.size() < 1) {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
					}
					else {
						if (ctx.hasAnswerText()) {
							ctx.appendNL();
						}
						ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
						ctx.appendAnswerText((String)params.get(0));
					}
					break;
				case set_menu: 
					if (params.size() < 2) {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
					}
					else {
						String menuKey = (String)params.get(0);
						String menuHeader = (String)params.get(1);
						String menuLifetime = (String)params.get(2);
						
						int menuStartIdx = 3;
						QAContext.Lifetime lt = null;
						MenuView menuView = MenuView.ORDERED_LIST;
						
						try {
							String ltStr = null;
							String menuViewStr = null;
							if (StringUtils.contains(menuLifetime, ':')) {
								ltStr = StringUtils.substringBefore(menuLifetime, ":");
								menuViewStr = StringUtils.substringAfter(menuLifetime, ":");
							}
							else {
								ltStr = menuLifetime;
							}
							lt = QAContext.Lifetime.valueOf(ltStr);
							
							if (menuViewStr != null) {
								menuView = MenuView.valueOf(menuViewStr);
							}
						}
						catch (Exception forBackwardCompatibility) {
							menuStartIdx = 2;
						}
						
						if (lt == null) {
							lt = QAContext.Lifetime.ONETIME;
						}
						
						OptionMenu menu = ctx.createOptionMenu(menuKey, menuView, lt, "_DLG_" + dlg.getMkey() ,MenuSelectionBehavior.NUMBER_OR_FULL_MATCH_TITLE, menuHeader, "\n", false, "  ");
						
						for(int i=menuStartIdx; i< params.size(); i++){
							try {
								String optionStr = params.get(i);
								String title = null;
								String question = null;
								if (StringUtils.contains(optionStr, ':')) {
									title = StringUtils.substringBefore(optionStr, ":");
									question = StringUtils.substringAfter(optionStr, ":");
								}
								else {
									title = optionStr;
									question = optionStr;
								}

								int optionNum = menu.addOption(QADialogPlugin.ID, null, OptionAction.INPUT_TEXT, title, new ParsedOption(title, question, OptionAction.INPUT_TEXT), null);
							}
							catch (Exception ignore) {
							}
						}

						if (menu.optionsSize() > 0)
							ctx.addAndShowOptionMenu(menu);
						//pres = PostRuleCheckResult.DEFAULT_RETURN_RESULT;
					}
					break;
				case append_menu: 
					if (params.size() < 2) {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
					}
					else {
						String menuKey = (String)params.get(0);
						String menuHeader = (String)params.get(1);
						OptionMenu menu = ctx.findOptionMenu(menuKey);
						if (menu == null) 
							menu = ctx.createOptionMenu(menuKey, MenuView.ORDERED_LIST, "_DLG_" + dlg.getMkey(), MenuSelectionBehavior.NUMBER_OR_FUZZY_TITLE, menuHeader, "\n", false, "  ");
						
						for(int i=2; i< params.size(); i++){
							try {
								String optionStr = params.get(i);
								String title = null;
								String question = null;
								if (StringUtils.contains(optionStr, ':')) {
									title = StringUtils.substringBefore(optionStr, ":");
									question = StringUtils.substringAfter(optionStr, ":");
								}
								else {
									title = optionStr;
									question = optionStr;
								}

								int optionNum = menu.addOption(QADialogPlugin.ID, null, OptionAction.INPUT_TEXT, title, new ParsedOption(title, question, OptionAction.INPUT_TEXT), null);
							}
							catch (Exception ignore) {
							}
						}

						if (menu.optionsSize() > 0)
							ctx.addAndShowOptionMenu(menu);
						//pres = PostRuleCheckResult.DEFAULT_RETURN_RESULT;
					}
					break;
				case add_line_template_message:
					if (params.size() < 1) {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
					}
					else {
						try {
							JSONObject jobj = (JSONObject)ctx.getResponseAttribute("line");
							if (jobj == null) {
								jobj = new JSONObject();
								ctx.setResponseAttribute("line", jobj);
							}
							if (!jobj.has("messages")) {
								jobj.put("messages", new JSONArray());
							}
							JSONArray msgs = jobj.getJSONArray("messages");
							
							for (Object o: params) {
								try {
									String mkey = (String)o;
									RichMessage msg = RichMessage.getByMKey(ctx.getTenant().getId(), mkey);
									
									if (msg != null) {
										msgs.add(new JSONObject(msg.getMsgTemplate(ctx)));
									}
								}
								catch (JSONException e) {
									e.printStackTrace();
								}
							}
						}
						catch (JSONException e) {
							e.printStackTrace();
						}
					}
					break;
				case rewrite_question: 
					// @NOT_IMPLEMENT
					break;
				case redirect_to_question: 
					if (params.size() < 1) {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
					}
					else {
    					val = params.get(0);
    					val = FunctionUtil.collectExecAndReplace(val, ctx);
    					if (StringUtils.trimToNull((String)val) != null) {
    						Long kid = QAUtil.id2Kid((String)val);
							QA pipe2 = new QA(ctx.getQAUtil().getMainQASolrDocument(kid));
        								
							if (pipe2 != null) {	                        
								dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction redirect to question [" + val + " => " + pipe2.getQuestion() + "].");

								// 結束此情境
								dlg.initialized = false;
								dlg.deactivate();
    								
								ctx.setQaAsAnswer(pipe2);
								pres = new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD, QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);  // 不是直接 RETURN，有答案還是應該要去跑 POST_QA_MATCH
							}
    					}
					}
					break;
				case redirect_to_node: 
					if (params.size() < 1) {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
					}
					else {
						DialogNode toNode = dlg.findDialogNode((String)params.get(0));
						
						if (toNode != null) {
							NodeState toState = NodeState.valueOf(params.get(1));
							pres = dlg.gotoNode(ctx, dlg, toNode, toState);
						}
						else {
							dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction [" + r.label + " : " + r.getType() + " : " + params + "], target node not found.");
						}
					}
					break;
				case redirect_to_last_node:
					if (dlg.lastNode != null) {
						NodeState toState = NodeState.valueOf(params.get(0));
						pres = dlg.gotoNode(ctx, dlg, dlg.lastNode, toState);
					}
					else {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction [" + r.label + " : " + r.getType() + " : " + params + "], target node not found.");
					}
					break;
				case set_dialog_variable: 
					if (params.size() < 2) {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
					}
					else {
						val = params.get(1);
						val = FunctionUtil.collectExecAndReplace(val, ctx);
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction set dialog variable [" + params.get(0) + " => " + val + "[" + r.label + " : " + r.getType() + " : " + params + "].");
						dlg.memory.put(params.get(0), StringUtils.trimToNull(val));
					}
					break;
				case set_context_variable: 
					if (params.size() < 2) {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
					}
					else {
						val = params.get(1);
						val = FunctionUtil.collectExecAndReplace(val, ctx);
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction set context variable [" + params.get(0) + " => " + val + "[" + r.label + " : " + r.getType() + " : " + params + "].");
						ctx.setCtxAttr(params.get(0), StringUtils.trimToNull(val));
					}
					break;
				 case set_context_variable_custom_qa:
	                    if (params.size() < 2) {
	                        dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
	                    }
	                    else {
	                        val = params.get(1);
	                        val = FunctionUtil.collectExecAndReplace(val, ctx);
	                        dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction set dialog variable [" + params.get(0) + " => " + val + "[" + r.label + " : " + r.getType() + " : " + params + "].");
	                        
	                        QAContext qaCtx = QAContextManager.create(java.util.UUID.randomUUID().toString());
	                        qaCtx.setTenant(ctx.getTenant());
	                        QAUtil qu = qaCtx.getQAUtil();
	                        qaCtx.setCurrentQuestion(val);
	                        
	                        try {
	                            QAOutputTemplate outputTpl = (QAOutputTemplate)QAOutputTemplate.findRegisteredClass("GeneralTextOutput").newInstance();
	                            SolrDocument customDoc = null;
	                            String preCQ = WiSeUtils.dblQuote(QAUtil.removeSymbols(qaCtx.getCurrentQuestion(), qaCtx.getTenant().getLocale()));
	                            if (!preCQ.equals("\" \"")){
	                                customDoc = qu.searchDirectCustomQA(qaCtx.getCurrentQuestion(), outputTpl, qaCtx);
	                            }
	                            String answer = qaCtx.getAnswerText().toString();
	                            ctx.setCtxAttr(params.get(0), StringUtils.trimToNull(answer));
	                        } catch (Exception e) {
	                            e.printStackTrace();
	                        }
	                    }
	                    
	                    break;
				case set_request_variable: 
					if (params.size() < 2) {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
					}
					else {
						val = params.get(1);
						val = FunctionUtil.collectExecAndReplace(val, ctx);
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction set request variable [" + params.get(0) + " => " + val + "[" + r.label + " : " + r.getType() + " : " + params + "].");
						ctx.setRequestAttribute(params.get(0), val);
					}
					break;
				case unset_dialog_variable: 
					if (params.size() < 1) {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
					}
					else {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction unset dialog variable [" + params.get(0) + " => " + r.label + " : " + r.getType() + " : " + params + "].");
						try {
							dlg.memory.remove(params.get(0));
						}catch(Exception ignoreIt) {}
					}
					break;
				case unset_context_variable: 
					if (params.size() < 1) {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
					}
					else {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction unset context variable [" + params.get(0) + " => " + r.label + " : " + r.getType() + " : " + params + "].");
						try {
							dlg.memory.remove(params.get(0));
						}catch(Exception ignoreIt) {}
						ctx.removeCtxAttr(params.get(0));
						Set<String> keySets = new HashSet<String>();
						keySets.addAll(ctx.getCtxAttr().keySet());
						for (String key : keySets) {
							if (key.contains(params.get(0))) {
								ctx.removeCtxAttr(key);
							}
						}
					}
					break;
				case unset_request_variable: 
					if (params.size() < 1) {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
					}
					else {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction unset request variable [" + params.get(0) + " => " + r.label + " : " + r.getType() + " : " + params + "].");
						ctx.removeRequestAttribute(params.get(0));
					}
					break;
				case groovy_script: 
					if (params.size() < 1) {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
					}
					else {
	        				Binding binding = new Binding();
	        				binding.setProperty("ctx", ctx);
	        				binding.setProperty("dlg", dlg);
	        				binding.setProperty("node", node);
	        				
	        				pres = (PostRuleCheckResult)GroovyUtil.runScript(binding, (String)params.get(0));
					}
        				break;
				case exit_dialog:
					dlg.initialized = false;
					dlg.deactivate();
					if (params.size() > 0) {
						String postResultCheckStatus = (String)params.get(0);
						PostRuleCheckResult.Status rs = PostRuleCheckResult.Status.valueOf(postResultCheckStatus);
						if (PostRuleCheckResult.Status.CONTINUE == rs) {
							pres = PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
						}
						else if (PostRuleCheckResult.Status.RETURN == rs) {
							pres = PostRuleCheckResult.DEFAULT_RETURN_RESULT;
						}
						else if (PostRuleCheckResult.Status.FORWARD == rs && params.size() > 1) {
							String forwardTo = (String)params.get(1);
							pres = new PostRuleCheckResult(rs, StringUtils.split(forwardTo, ","));
						}
					}
					else {
						pres = PostRuleCheckResult.DEFAULT_RETURN_RESULT;
					}
					//QADialogRule.removeFromRunningDialogList(ctx, dlg);
					break;
				case check_context_append: 
					if (params.size() < 3) {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
					}
					else {
						if (params.size() % 2 != 1 && params.size() < 3)
							break;
						if (ctx.hasAnswerText()) {
							ctx.appendNL();
						}
						ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
						boolean ctxExist = false;
						for (int i = 0; i < params.size(); i = i + 2) {	
							val = params.get(i);
							int valLegth = val.split(";").length;
							if (i == (params.size() - 1) && ctxExist == false) {
								val = FunctionUtil.collectExecAndReplace(val, ctx);
								ctx.appendAnswerText((String) val);
							}
							if (i == (params.size() - 1)) {
								continue;
							}
							val = FunctionUtil.collectExecAndReplace(val, ctx);
							if (val.length() > 0 && !params.get(i).contains(val)) {
								String[] val1s = val.split(";");
								if(valLegth != val1s.length)
									continue;
								val = params.get(i+1);
								for(int j = 0;j<val1s.length;j++){
									val = val.replace("VALREPLACE"+j, val1s[j]);
								}
								val = FunctionUtil.collectExecAndReplace(val, ctx);
								ctx.appendAnswerText(val);
								ctxExist = true;
							}
						}
					}
					break;
				case set_context_variable_get_date:
					if (params.size() < 2) {
                        dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
                    }
                    else {
                        val = params.get(1);
                        val = FunctionUtil.collectExecAndReplace(val, ctx);
                        dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction set dialog variable [" + params.get(0) + " => " + val + "[" + r.label + " : " + r.getType() + " : " + params + "].");
                        
                        Date date = new Date();
                        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
                        if (val.equals("明天")) {
                        	date.setDate(date.getDate() + 1);
                        } else if (val.equals("後天")) {
                        	date.setDate(date.getDate() + 2);
                        } else if (val.equals("下禮拜")) {
                        	date.setDate(date.getDate() + 7);
                        } else if (val.equals("下個月")) {
                        	date.setDate(date.getDate() + 30);
                        }
                        ctx.setCtxAttr(params.get(0), sdf.format(date));
                    }
					break;
                case set_response_variable:
                    if (params.size() < 2) {
                        dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
                    }
                    else {
                        val = params.get(1);
                        val = FunctionUtil.collectExecAndReplace(val, ctx);
                        dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction set response variable [" + params.get(0) + " => " + val + "[" + r.label + " : " + r.getType() + " : " + params + "].");
                        ctx.setResponseAttribute(params.get(0), StringUtils.trimToNull(val));
                    }
                    break;
                case add_tag_to_user:
					if (params.size() < 1) {
						dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction contents not enough [" + r.label + " : " + r.getType() + " : " + params + "].");
					}
					else {
						val = params.get(0);
                    		UserClue clue = ctx.getUserClue();
                    		if (clue != null) {
                    			UserClueTag ut = UserClueTag.addTagToUser(clue, val);
                            dlg.currentLog.appendNL().appendLog("Dialog[" + dlg.name + "] reaction add tag to user [" + clue.toString() + " => " + val + "[" + r.label + " : " + r.getType() + " : " + params + "].");
                    		}
					}
                		break;
			}
			
			return pres;
		}
		public void setLabel(String label) {
			this.label = label;
		}
		public NodeState getWhen() {
			return when;
		}
		public void setWhen(NodeState when) {
			this.when = when;
		}
		public ReactionType getType() {
			return type;
		}
		public void setType(ReactionType type) {
			this.type = type;
		}
		public List<String> getContents() {
			return contents != null ? contents : EMPTY_LIST;
		}
		public void setContents(List<String> params) {
			this.contents = params;
		}
		public String getDetail() {
			return detail;
		}
		public void setDetail(String detail) {
			this.detail = detail;
		}
		public Boolean getEnable() {
			return enable != null ? enable : false;
		}
		public void setEnable(Boolean enable) {
			this.enable = enable;
		}
	}

	public static enum NodeType {
		NORMAL, 
		SYMBOLIC_LINK_TO_NODE, 
		TRANSPARENT,
	}
	
	public static enum NodeState {
		JUST_ACTIVATED,
		WAIT_INPUT,
		GOT_INPUT,
		LEAVING_NODE,
		DEACTIVE,
	}
	
	public static enum PerceptionType {
		regex, 
		confirm,
		number_range,
		string,
		direct_answer,
		date_time,
		address,
		intent_entity, 
		groovy_script, 
		unconditional,
		identity_number,
		intent_entity_exit,
		inline_function,
	}
	
	public static enum ReactionType {
		set_answer, 				// 覆蓋答案, param[0]=(String)answer
		append_answer, 			// 附加答案, param[0]=(String)answer
		set_menu, 				// 覆蓋選項, param[0]=(String)menu_key, [1]=(String)menu_header, [2+N]=(String)選項標題:選項值
		append_menu, 			// 附加選項, param[0]=(String)menu_key, [1]=(String)menu_header, [2+N]=(String)選項標題:選項值
		add_line_template_message, // 加入LINE圖文， param[0]=(String)mkey
		rewrite_question,		// 改寫問題, param[0]=(String)rewrite_rule
		redirect_to_question, 	// 轉送到某個問答, param[0]=(String)question id
		redirect_to_node, 		// 轉送到某個節點（這裡視為 currentNode 改為該節點，因此要重新比對該節點的 subNodes 的 perceptions, param[0]=node id
		redirect_to_last_node, 	// 轉送到上個節點（這裡視為 currentNode 改為該節點，因此要重新比對該節點的 subNodes 的 perceptions, param[]=?
		set_dialog_variable, 	// 設定變數（dialog scope), param[0]=variable name, [1]=value object
		set_context_variable, 	// 設定變數（context scope), param[0]=variable name, [1]=value object
		set_context_variable_custom_qa,        // 設定長期變數(搜尋特殊問答)
		set_request_variable, 	// 設定變數（request scope), param[0]=variable name, [1]=value object
		unset_dialog_variable, 	// 清空變數（dialog scope), param[0]=variable name
		unset_context_variable, 	// 清空變數（context scope), param[0]=variable name
		unset_request_variable, 	// 清空變數（request scope), param[0]=variable name
		do_search, 				// 查詢索引, param[0]=(String)result_name, [1+N]=(String)solr_param_name, [2+N]=(String)solr_value 
		groovy_script, 			// 執行 groovy_script, param[0]=script text
		exit_dialog, 			// 
		trigger_node, 			//
		check_context_append,	// 檢查長期變數
		set_context_variable_get_date,	// 取得日期
        set_response_variable,	// 設定變數（response scope), param[0]=variable name, [1]=value object
        add_tag_to_user,			// 替 User 貼上標籤
	}
	
	static enum Operator { SHOULD, MUST, MUSTNOT }
	
	/**
	 * 這裡偷懶把測試案例寫在 main()...
	 * @param args
	 */
	public static void main(String[] args) {
		// TestCases
		Tenant t = new Tenant();
		t.setId(0);
		QAContext ctx = new QAContext();
		ctx.setTenant(t);
		ctx.setCurrentQuestion("我想換30000元");
		ctx.setCtxAttr("FOO", "foo in CTX");
		ctx.setRequestAttribute("BAR", "bar in REQ");
		ctx.setResponseAttribute("BOO", "bar in RESPONSE");
		
		Object[][] testCases = new Object[][] {
			// QUESTION, Set<Intent>, Set<Entity>, PerceptionType, criteria, expect result 
			{"我想換30000元", null, null, PerceptionType.regex, ".*?[0-9]+.*?", true },
			{"我想換30000元", null, null, PerceptionType.regex, "[0-9]+", false },
			{"我想換30000元", createTestIntentSet("WHAT", "WHERE"), null, PerceptionType.intent_entity, "+@WHAT", true },
			{"我想換30000元", createTestIntentSet("WHAT", "WHERE"), null, PerceptionType.intent_entity, "+@WHAT +@WHERE", true },
			{"我想換30000元", createTestIntentSet("WHAT", "WHERE"), null, PerceptionType.intent_entity, "+@WHAT +@WHERE @ABC", true },
			{"我想換30000元", createTestIntentSet("WHAT", "WHERE"), null, PerceptionType.intent_entity, "+@WHAT -@WHERE @ABC", false },
			{"我想換30000元", createTestIntentSet("WHAT", "WHERE"), null, PerceptionType.intent_entity, "+@WHAT -@WHERE", false },
			{"我想換30000元", createTestIntentSet("WHAT", "WHERE"), null, PerceptionType.intent_entity, "@ABC", false },
			{"我想換30000元", createTestIntentSet("WHAT"), createTestEntitySet("MONEY", "ME"), PerceptionType.intent_entity, "-@WHAT +#ME", false },
			{"我想換30000元", createTestIntentSet("WHAT"), createTestEntitySet("MONEY", "ME"), PerceptionType.intent_entity, "@ABC @DEF #ME", true },
			{"我想換30000元", createTestIntentSet("WHAT"), createTestEntitySet("MONEY", "ME"), PerceptionType.intent_entity, "@ABC @DEF @ME", false },
			{"我想換30000元", createTestIntentSet("WHAT", "WHERE"), null, PerceptionType.intent_entity, "+@WHAT +%_CTX_FOO", true },
			{"我想換30000元", createTestIntentSet("WHAT", "WHERE"), null, PerceptionType.intent_entity, "+@WHAT -%_CTX_FOO", false },
			{"我想換30000元", createTestIntentSet("WHAT", "WHERE"), null, PerceptionType.intent_entity, "+@WHAT +%_CTX_FOO +%_REQ_BAR", true },
			{"我想換30000元", createTestIntentSet("WHAT", "WHERE"), null, PerceptionType.intent_entity, "+@WHAT +%_CTX_FOO -%_REQ_BARR", true },
			{"我想換30000元", createTestIntentSet("WHAT", "WHERE"), null, PerceptionType.intent_entity, "+@WHAT +%_CTX_FOO -%_REQ_BAR", false },
			{"我想換30000元", createTestIntentSet("WHAT", "WHERE"), null, PerceptionType.intent_entity, "+@WHAT +%_REP_FOO", false },
			{"我想換30000元", createTestIntentSet("WHAT", "WHERE"), null, PerceptionType.intent_entity, "+@WHAT +%_REP_BOO", true },
			{"我想換30000元", null, createTestEntitySet("AREA:日本", "TYPE:五日"), PerceptionType.inline_function, "{{CMP:eq::[[MATCHEDENTITYVAL:AREA]]::日本}}", true },
			{"我想換30000元", null, createTestEntitySet("AREA:日本", "TYPE:五日"), PerceptionType.inline_function, "{{CMP:eq::[[MATCHEDENTITYVAL:AREA]]::美國}}", false },
		};
		
		
		QAConversationalDialog dlg = new QAConversationalDialog();
		
		int i = 1;
		for (Object[] testCase: testCases) {
			Perception p = new Perception();
			// 測試正規表示法
			ctx.setCurrentQuestion((String)testCase[0]);
			ctx.setIntents((Set<QAIntent>)testCase[1]);
			ctx.setEntities((Set<QAEntity>)testCase[2]);
			p.setType((PerceptionType)testCase[3]);
			p.setCriteria((String)testCase[4]);
			boolean result = p.check(ctx, dlg, null);
			boolean failed = (result != (boolean)testCase[5]);
			
			System.out.println(String.format(
					"[%s] (%d). Input[%s] / Intent[%s] / Entity[%s]\nTest[%s / %s / expect (%b) got (%b)",
					failed ? "X" : "O",
					i++,
					testCase[0], testCase[1], testCase[2], testCase[3], testCase[4], testCase[5], result
					));
		}
	}

	/**
	 * 單純用來產生測試需要的 intents
	 * @param tags
	 * @return
	 */
	static Set<QAIntent> createTestIntentSet(String... tags) {
		Set<QAIntent> s = new HashSet<QAIntent>();
		long i=0;
		for (String tag: tags) {
			QAIntent t = new QAIntent();
			t.setId(i++);
			t.setTag(tag);
			s.add(t);
		}
		
		return s;
	}
	/**
	 * 單純用來產生測試需要的 entities
	 * @param codes
	 * @return
	 */
	static Set<QAEntity> createTestEntitySet(String... codeValPairs) {
		Set<QAEntity> s = new HashSet<QAEntity>();
		long i=0;
		for (String p: codeValPairs) {
			String code = StringUtils.substringBefore(p, ":");
			String val = StringUtils.substringAfter(p, ":");
			QAEntity t = new QAEntity();
			t.setId(i++);
			t.setCode(code);
			t.setEntityValues(val);
			s.add(t);
		}
		
		return s;
	}
	
	private static class CollectionUtil {
        public static interface Mapper<T, U> {
            public U map(T item);
        }
        public static <T, U> List<U> map(Iterable<T> list, Mapper<T, U> mapper) {
            List<U> result = new ArrayList<U>();
            for (T item : list) {
                result.add(mapper.map(item));
            }
            return result;
        }
    }
}