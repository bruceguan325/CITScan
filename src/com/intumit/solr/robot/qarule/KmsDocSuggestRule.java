package com.intumit.solr.robot.qarule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.TemplateUtil;
import com.intumit.solr.robot.TemplateUtil.Replacer;

public class KmsDocSuggestRule implements PostQAMatchRule {
	@Override
	public void init(Map<String, Object> configs) {

	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		QA directAnswer = ctx.getCurrentQA();
		if(directAnswer != null && !directAnswer.getBinderIds().isEmpty()) {
			return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
		}
		return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT ;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		QA directAnswer = ctx.getCurrentQA();
		if(directAnswer != null && !directAnswer.getBinderIds().isEmpty()) {
			List<Object> binderIds = new ArrayList<Object>();
			binderIds.addAll(directAnswer.getBinderIds());
			ctx.setResponseAttribute("kmsDocs", formatKmsDocument(ctx, binderIds, directAnswer.getKid()));
		}
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx, PostRuleCheckResult result) {
		return result;
	}

	private static final Integer MAX_KMS_DOC_SIZE = Integer.valueOf(3);

	private String formatKmsDocument(QAContext ctx, List<Object> binderIds, Long kid) {
		StringBuffer resp = new StringBuffer();
		if(binderIds == null) { return resp.toString(); }
		String format = "{{L:%s:%s}}" + (ctx.isClientSupportHtml()?"<br/>":"\\n");
		if(StringUtils.isBlank(ctx.getTenant().getKmsUrl())) {
			return resp.toString();
		}
        StringBuffer url = new StringBuffer(ctx.getTenant().getKmsUrl());
        url.append("/api/listDocument.jsp?binderIds=").append(StringUtils.join(binderIds, ","));
        url.append("&kid=").append(kid);
        try {
            HttpClient httpclient = new HttpClient();
            GetMethod get = new GetMethod(url.toString());
            httpclient.getParams().setSoTimeout(5 * 1000);
            httpclient.executeMethod(get);
            JSONObject respsone = new JSONObject(StringUtils.trim(get.getResponseBodyAsString()));
            if(respsone.has("docs")) {
            	JSONArray docs = respsone.getJSONArray("docs");
            	for(int i = 0 ; i < docs.length() && i < MAX_KMS_DOC_SIZE ; i++) {
            		JSONObject doc = docs.getJSONObject(i);
            		resp.append(String.format(format, (i+1) + ". " + doc.optString("title"), doc.optString("link")));
            	}
            }

            if(respsone.has("more")) {
            	resp.append(String.format(format, "more...", respsone.optString("more")));
            }
        }
        catch (Exception ignore) {
            ignore.printStackTrace();
        }

        return TemplateUtil.process(resp.toString(), new Replacer() {
			@Override
			public String call(String name, String val) {
				if (StringUtils.equals(name, "L")) {
					String linkName = StringUtils.defaultString(StringUtils.trimToNull(StringUtils.substringBefore(val, ":")), "連結");
					String link = StringUtils.substringAfter(val, ":");
					return "<a href=\"#\" title=\"" + linkName + "\" class=\"al kms\" target=\"" + QAUtil.ANSWER_LINK_TARGET + "\" data-url='" + link + "'>" + linkName + "</a>";
				}
				return null;
			}

			@Override
			public String call(QAContext ctx, String name, String val) {
				if (StringUtils.equals(name, "L")) {
					String linkName = StringUtils.defaultString(StringUtils.trimToNull(StringUtils.substringBefore(val, ":")), "連結");
					String link = StringUtils.substringAfter(val, ":");
					return "<a href=\"#\" title=\"" + linkName + "\" class=\"al kms\" target=\"" + QAUtil.ANSWER_LINK_TARGET + "\" data-url='" + link + "'>" + linkName + "</a>";
				}
				return null;
			}
        });
	}
}
