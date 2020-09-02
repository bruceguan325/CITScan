package com.intumit.solr.robot;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;

import com.intumit.solr.config.ColumnNameMappingFacade;

public class QAPatternUtil {

	public static String applyQAPatternTemplate(QAContext ctx, QAPattern qp, SolrQuery query) {
		StringWriter buf = new StringWriter();
		QAOutputResult r = new QAOutputResult();
		
		try {
			//取得velocity的上下文context
			VelocityContext context = new VelocityContext();
			try {
				QueryResponse res = QADataAggregator.getDataSourceServer(qp.getDataSource(), ctx.getTenant()).query(query);
				SolrDocumentList docs = res.getResults();
				
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
				
				//把數據填入上下文
				context.put("docs", docs);
				context.put("question", ctx.getCurrentQuestion());
				context.put("qaCtx", ctx);
				context.put("math", new MathTool());
				context.put("num", new NumberTool());
				context.put("date", new DateTool());
				context.put("colMapper", ColumnNameMappingFacade.getInstance());
			}
			catch (SolrServerException e) {
				e.printStackTrace();
			}

			//轉換輸出
			VelocityEngine ve = new VelocityEngine();

			// Make Velocity log to Log4J
			ve.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS,
			"org.apache.velocity.runtime.log.SimpleLog4JLogSystem");

			ve.setProperty("runtime.log.logsystem.log4j.category", QAPatternUtil.class.getName());

			ve.init();
			ve.evaluate(context, buf, QAPatternUtil.class.getName(), qp.getAnswerTemplate());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
			
		String ans = buf.toString();
		return ans;
	}
}
