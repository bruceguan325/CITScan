package com.intumit.solr.robot.function;

import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.intumit.solr.robot.QAContext;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

/**
 * Support JsonPath {@link http://goessner.net/articles/JsonPath/}
 * 
 * param[0] = json path
 * param[1] = defVal if null
 * param[2] = json string (or user input)
 * 
 * @author herb
 *
 */
public class GETJSONVAL extends FunctionBase {
	Configuration conf = null;

	public GETJSONVAL(String originalText, String data) {
		super(originalText, data);
		this.originalText = originalText;
		this.data = data;
		
		conf = Configuration.defaultConfiguration();
		conf.addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		if (data.contains("[[") && data.contains("]]")) {
			data = data.replace("[[", "{{").replace("]]", "}}");
			data = FunctionUtil.collectExecAndReplace(data, ctx);
		}
		
		List<String> params = splitData(3);
		String path = params.get(0);
		String defVal = params.get(1);
		String jsonStr = params.get(2);
		
		if (in != null) {
			jsonStr = in.getInput();
		}
		
		Object j = null;
		
		try {
			j = JsonPath.using(conf).parse(jsonStr).read(path);
			
			if (j != null && j instanceof Collection) {
				// 多個結果，全部接在一起（沒間隔）
				final StringBuilder sb = new StringBuilder();
				CollectionUtils.forAllDo((Collection)j, x -> { if (sb.length() > 0) sb.append(", "); sb.append(x);});
				j = sb.toString();
			}
		}
		catch (com.jayway.jsonpath.PathNotFoundException e) {
			System.out.println("JsonPath not found:" + e.getMessage());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return j != null ? j : defVal;
	}
}
