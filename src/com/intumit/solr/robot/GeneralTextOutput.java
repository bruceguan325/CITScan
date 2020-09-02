package com.intumit.solr.robot;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;

import com.intumit.solr.config.ColumnNameMappingFacade;
import com.intumit.solr.robot.dictionary.CustomData;

public class GeneralTextOutput extends QAOutputTemplate {

	public QAOutputResult output(QA customQa, QAContext qaCtx, QAPattern qp, List<CustomData> nvPairs, QADataAggregator aggregator) {
		StringWriter buf = new StringWriter();
		StringWriter bufVoice = new StringWriter();
		QAOutputResult r = new QAOutputResult();
		
		try {
			//取得velocity的上下文context
			VelocityContext context = new VelocityContext();
			aggregator.aggregate(customQa, qaCtx, qp, nvPairs, context);
			
			SolrDocumentList docs = (SolrDocumentList)context.get("docs");
			AtomicBoolean autoCompleted = context.get("autoCompleted") != null ? (AtomicBoolean)context.get("autoCompleted") : new AtomicBoolean(false);
			

			if (qaCtx.getTenant().getEnableQAExplain()) {
				qaCtx.appendExplain(this.getClass().getName(),
					String.format("aggregator.aggregated docs (%d): %s",
							(docs == null ?0:docs.getNumFound()), docs ));
			}

			
			if (docs != null && docs.size() > 0) {
				r.setHasResult(true);
				SolrDocument firstDoc = docs.get(0);
				List<String> visibleFn = new ArrayList<String>();
				
				for (String fn: firstDoc.getFieldNames()) {
					if (!fn.endsWith("_s") && !fn.endsWith("_ms")) continue;
					if (fn.equalsIgnoreCase("dataType_s")) continue;
					visibleFn.add(fn);
				}
				context.put("fields", visibleFn);
			}
			else {
				r.setHasResult(false);
			}
			r.setAutoCompleted(autoCompleted.get());
			
			//把數據填入上下文
			context.put("question", qaCtx.getCurrentQuestion());
			context.put("qaCtx", qaCtx);
			context.put("math", new MathTool());
			context.put("num", new NumberTool());
			context.put("date", new DateTool());
			context.put("colMapper", ColumnNameMappingFacade.getInstance());

			//轉換輸出
			VelocityEngine ve = new VelocityEngine();

			// Make Velocity log to Log4J
			ve.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS,
			"org.apache.velocity.runtime.log.SimpleLog4JLogSystem");

			ve.setProperty("runtime.log.logsystem.log4j.category",
					this.getClass().getName());

			ve.init();
			
			MultiChannelAnswer mca = MultiChannelAnswer.findNonEmptyAnswer(qaCtx, customQa.getId(), qaCtx.getQAChannelInstance(), qaCtx.getUserType());
			String channelSpecificAnswerTemplate = null;
			
			try {
				if (mca != null && QAChannelType.VELOCITY_TEMPLATE == mca.getQAChannel().getType()) {
					channelSpecificAnswerTemplate = StringUtils.trimToNull(mca.getAnswer());
					
					if (channelSpecificAnswerTemplate != null) {
						System.out.println("GenerateTextOuput of " + customQa.getId() + " in channel[" + qaCtx.getQaChannel() + "] ut[" + qaCtx.getUserType() + "] using specificAnswerTemplate [" + channelSpecificAnswerTemplate + "]");
					}
				}
			}
			catch (Exception ignoreIt) {}

			ve.evaluate(context, buf, this.getClass().getName(), channelSpecificAnswerTemplate != null ? channelSpecificAnswerTemplate : qp.getAnswerTemplate());
			
			if (StringUtils.trimToNull(qp.getAnswerVoice()) != null) {
				ve.evaluate(context, bufVoice, this.getClass().getName(), qp.getAnswerVoice());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		qaCtx.setResponseAttribute("outputVoice", bufVoice.toString());
		r.setOutput(buf.toString());
		return r;
	}

}
