package com.intumit.solr.robot.function;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.entity.QAEntity;

public class DATECALCULATE extends FunctionBase {

	public DATECALCULATE(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		if (data.contains("[[") && data.contains("]]")) {
			data = data.replace("[[", "{{").replace("]]", "}}");
			data = FunctionUtil.collectExecAndReplace(data, ctx);
		}
		String result = "";
		List<String> params = splitData(); 
		String date = params.get(0);
		Pattern pp = Pattern.compile("([\\+\\-])(.*)");
		Matcher mm = pp.matcher(params.get(1));
		while(mm.find()){
			String condition = mm.group(1);
			String days = mm.group(2);
			SimpleDateFormat sdf = null;
			if(date.length() > 10) {
				sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
			} else {
				sdf = new SimpleDateFormat("yyyy/MM/dd");
			}
			Date d = null;;
			try {
				d = sdf.parse(date);
			} catch (ParseException e) {
			}
			Calendar cal = Calendar.getInstance();
			cal.setTime(d);
			cal.add(Calendar.DATE, Integer.valueOf(condition+days));
			result = sdf.format(cal.getTime());
		}
		return result;
	}
}
