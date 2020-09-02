package com.intumit.solr.robot.function;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.MenuSelectionBehavior;
import com.intumit.solr.robot.QAContext.MenuView;
import com.intumit.solr.robot.QAContext.OptionAction;
import com.intumit.solr.robot.QAContext.OptionMenu;
import com.intumit.solr.robot.qaplugin.ParsedOption;

public class APPENDOPTION extends FunctionBase {

	public APPENDOPTION(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		List<String> params = null;
		if (StringUtils.indexOf(data, "::") == -1) {
			params = splitDataForSingleColon();
			System.out.println("The {{OPTIONS:OOO:XXX}} format should be change to {{OPTIONS:OOO::XXX}} (p.s. Double colons between 'OOO' and 'XXX')");
		}
		else {
			params = splitData();
		}
		
		if (params != null && params.size() == 2) {
			String title = params.get(0);
			String option = params.get(1);
	
			String menuKey = "INLINE_OPTIONS";
			boolean newMenu = false;
			OptionMenu menu = ctx.findOptionMenu(menuKey);
			
			if (menu == null) {
				newMenu = true;
				String createFrom = "_CLS_" + getClass().getName();
				if (ctx.getCurrentQA() != null) {
					createFrom = ctx.getCurrentQA().getId();
				}
				menu = ctx.createOptionMenu(menuKey, MenuView.ORDERED_LIST, QAContext.Lifetime.ONETIME, createFrom, MenuSelectionBehavior.NUMBER_OR_FUZZY_TITLE, "", "\n", false, "  ");
			}
			
			int optionNum = menu.addOption(null, null, OptionAction.INPUT_TEXT, title, new ParsedOption(title, option, OptionAction.INPUT_TEXT), null);
	
			if (menu.optionsSize() > 0) {
				if (newMenu) 
					ctx.addAndShowOptionMenu(menu, 0);
				else
					ctx.showOptionMenu(menuKey, 0);
			}
		}
		else {
			System.out.println("Inline function [APPENDOPTION] need correct parameters. Current input [" + data + "] is wrong.");
		}
		
		return "";
	}
	

	
	List<String> splitDataForSingleColon() {
		return Arrays.asList(StringUtils.splitByWholeSeparatorPreserveAllTokens(data, ":"));
	}
}
