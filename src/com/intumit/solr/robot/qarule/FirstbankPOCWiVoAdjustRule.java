package com.intumit.solr.robot.qarule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.dictionary.KnowledgePointDictionary;
import com.intumit.solr.robot.dictionary.bank.DepositInterestRateDict;
import com.intumit.solr.robot.dictionary.bank.DepositInterestRateDict.Purpose;

public class FirstbankPOCWiVoAdjustRule implements PreQAMatchRule {
	private static final Logger LOG = LoggerFactory.getLogger(FirstbankPOCWiVoAdjustRule.class);
	
	public static int DEFAULT_LIMIT = 5;
	int limit = DEFAULT_LIMIT;
	
	public FirstbankPOCWiVoAdjustRule() {
		super();
	}

	public FirstbankPOCWiVoAdjustRule(Map<String, Object> configs) {
		super();
		init(configs);
	}

	@Override
	public void init(Map<String, Object> configs) {
		if (configs.containsKey("limit")) {
			limit = (Integer)configs.get("limit");
		}
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {

		String tmpQ = StringUtils.lowerCase(StringUtils.trimToEmpty(ctx.getCurrentQuestion()));
		String suggestQ = ctx.getTenant().getEnablePhoneticHomonym() 
						? KnowledgePointDictionary.suggestFullQuestion(ctx.getTenant().getId(), tmpQ)
						: null;
						
		if (suggestQ != null) {
			ctx.setCurrentQuestion(suggestQ);
			LOG.info("WiVO adjust sentence " + tmpQ + " => " + suggestQ);
		}
		DepositInterestRateDict[] pieces = DepositInterestRateDict.search(ctx, ctx.getTenant().getId(), ctx.getCurrentQuestion().toCharArray());
		
		if (pieces != null && pieces.length > 0) {
			System.out.println(Arrays.asList(pieces));
			DepositInterestRateDict depositType = null;
			DepositInterestRateDict currencyType = null;
			DepositInterestRateDict timeRange = null;
			DepositInterestRateDict rates = null;
			
			int mainPieceCount = 0;
			List<DepositInterestRateDict> others = new ArrayList<DepositInterestRateDict>();
			
			for (DepositInterestRateDict dird: pieces) {
				if (depositType == null && dird.getPurposeSet().contains(DepositInterestRateDict.Purpose.DEPOSIT_TYPE)) {
					mainPieceCount++;
					depositType = dird;
				}
				else if (currencyType == null && dird.getPurposeSet().contains(DepositInterestRateDict.Purpose.CURRENCY_TYPE)) {
					mainPieceCount++;
					currencyType = dird;
				}
				else if (timeRange == null && dird.getPurposeSet().contains(DepositInterestRateDict.Purpose.TIME_RANGE)) {
					mainPieceCount++;
					timeRange = dird;
				}
				else if (rates == null && dird.getPurposeSet().contains(DepositInterestRateDict.Purpose.RATE)) {
					mainPieceCount++;
					rates = dird;
				}
				else if (dird.getPurposeSet().contains(DepositInterestRateDict.Purpose.OTHERS)) {
					others.add(dird);
				}
			}
			
			if (mainPieceCount == 1 && StringUtils.isNotEmpty(ctx.getLastQuestion())) {
				pieces = DepositInterestRateDict.search(ctx, ctx.getTenant().getId(), ctx.getLastQuestion().toCharArray());

				if (pieces != null && pieces.length > 0) {
					boolean allowLastOthers = others.size() == 0;
					
					for (DepositInterestRateDict dird: pieces) {
						if (depositType == null && dird.getPurposeSet().contains(DepositInterestRateDict.Purpose.DEPOSIT_TYPE)) {
							mainPieceCount++;
							depositType = dird;
						}
						else if (currencyType == null && dird.getPurposeSet().contains(DepositInterestRateDict.Purpose.CURRENCY_TYPE)) {
							mainPieceCount++;
							currencyType = dird;
						}
						else if (timeRange == null && dird.getPurposeSet().contains(DepositInterestRateDict.Purpose.TIME_RANGE)) {
							mainPieceCount++;
							timeRange = dird;
						}
						else if (rates == null && dird.getPurposeSet().contains(DepositInterestRateDict.Purpose.RATE)) {
							mainPieceCount++;
							rates = dird;
						}
						else if (allowLastOthers && dird.getPurposeSet().contains(DepositInterestRateDict.Purpose.OTHERS)) {
							others.add(dird);
						}
					}
				}
			}
			
			if (depositType != null || rates != null) {
				String ns = "";
				if (currencyType != null) {
					ns += currencyType.getKeyword();
				}
				if (depositType != null) {
					ns += depositType.getKeyword();
				}
				if (timeRange != null) {
					ns += timeRange.getKeyword();
				}
				if (rates != null) {
					ns += rates.getKeyword();
				}
				for (DepositInterestRateDict dird: others) {
					ns += dird.getKeyword();
				}

				if (!("幣別定存".equals(ns) || ("幣別活存".equals(ns))))  {
					LOG.info("DepositInterestRateDict adjust sentence " + ctx.getCurrentQuestion() + " => " + ns);
					ctx.setCurrentQuestion(ns);
				}
			}
			else {
			}
		}
		
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx, PostRuleCheckResult result) {
		// Do nothing
		return result;
	}

}
