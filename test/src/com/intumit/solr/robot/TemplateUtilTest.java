package com.intumit.solr.robot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.intumit.solr.robot.TemplateUtil.Prepend;
import com.intumit.solr.robot.TemplateUtil.Replacer;
import com.intumit.solr.robot.function.FunctionCollector;

public class TemplateUtilTest {

	public static void main(String[] args) throws IOException {
		String tpl = FileUtils.readFileToString(
			new File("test/test-files/template/shortcut.txt"));
		System.out.println("----------template-----------");
		System.out.println(tpl);
		System.out.println("-----------------");
		TemplateUtil.process(tpl, new Replacer() {
			@Override
			public String call(String name, String val) {
				System.out.println("name: " + name + ", value: " + val);
				return null;
			}

			@Override
			public String call(QAContext ctx, String name, String val) {
				return call(name, val);
			}
		});
		System.out.println("------------output-----------");
		String prefix = "http://www.mybank.com.tw/";
		String tagName = "SHORTCUT";
		String out = TemplateUtil.processByTagName(tpl, tagName, new Prepend(prefix));
		System.out.println(out);
		
		String wholeNameOnlyTpl = " {{ LHCSPCODE3501 }}";
		String wholeNameOnlyTpl2 = " {{ LHCSPCODE3501 }} b ";
		String wholeNameOnlyTpl3 = " {{ LHCSPCODE3501 }}  {{ LHCSPCODE3501 }}";
		String tagName2 = "LHCSPCODE3501";
		System.out.println("----------name only template-----------");
		System.out.println(wholeNameOnlyTpl);
		System.out.println("result 1: " + TemplateUtil.matchWholeByTagName(wholeNameOnlyTpl, tagName2));
		System.out.println(wholeNameOnlyTpl2);
		System.out.println("result 2: " + TemplateUtil.matchWholeByTagName(wholeNameOnlyTpl2, tagName2));
		System.out.println(wholeNameOnlyTpl3);
		System.out.println("result 3: " + TemplateUtil.matchWholeByTagName(wholeNameOnlyTpl3, tagName2));
		
		String nameOnlyTpl = "大家好我叫做{{ROBOT_NAME}}，請多多照顧我喔！";
		String nameOnlyTag = "ROBOT_NAME";
		System.out.println("result 4: " + TemplateUtil.processByTagName(nameOnlyTpl, nameOnlyTag, new TemplateUtil.Replace("小智")));

		/*System.out.println("----------qapattern template-----------");
		String qapatternTpl1 = "{{$FUND_NAME}}的[基金語音代碼|基金代碼]為何{{$+FUND_NAME}}?";
		System.out.println(TemplateUtil.process(qapatternTpl1, TemplateUtil.CUSTOM_QA_REPLACER));*/
		
		TemplateUtil.processByTagName("請到 {{L:這裡下載:http://www.capital.com.tw/support/CFEContent/ContentAtt/{6056D256-9841-445A-8066-3CCAE4FC7944}/群益掌中財神手冊.pdf}} ，記住是 PDF 檔喔", "L", new Replacer() {
			@Override
			public String call(String name, String val) {
				if (StringUtils.equals(name, "L")) {
					String linkName = StringUtils.substringBefore(val, ":");
					String link = StringUtils.substringAfter(val, ":");
					
					System.out.println( "<a href=\"" + link + "\" title=\"" + linkName + "\" class=\"al\" target=\"" + QAUtil.ANSWER_LINK_TARGET + "\">" + linkName + "</a>" );
				}
				return null;
			}

			@Override
			public String call(QAContext ctx, String name, String val) {
				return call(name, val);
			}
		});
	}
	
}
