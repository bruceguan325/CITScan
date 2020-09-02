package com.intumit.solr.robot.function;

import java.util.List;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.qadialog.QAConversationalDialog;
import com.intumit.solr.robot.qadialog.QADialog;
import com.intumit.solr.robot.qarule.QADialogRule;

public class GETDLG extends FunctionBase {

	public GETDLG(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {

		List<String> params = splitData();
		String mkey = params.get(0);
		String key = params.get(1);
		QADialog dlg = QADialogRule.findRunningDialog(ctx, mkey);
		
		if (dlg != null && dlg instanceof QAConversationalDialog) {
			return ((QAConversationalDialog) dlg).getDlgAttr(key);
		}
		
		return null;
	}
}
