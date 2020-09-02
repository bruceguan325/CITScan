
/**
 * 需要 JQuery UI Dialog
 * 需要 srbt-common.js
 * 同時需要 typed.js （呈現打字效果）
 * 
 * start 傳入的 data 為 json object 必需有 q 以及 correctKid 兩個 property
 */
(function(global) {

	function QAUtility(data) {
		this.config = $.extend(data, this.config);
		return this;
	}

	QAUtility.prototype.config = {
		docId : 0,
		enable : false
	};
	
	QAUtility.prototype.testQ = function(question, correctKid, stat) {
		return 
	};

	QAUtility.prototype.merge = function(data, callback) {
		if (!$('#magic-dialog').length) {
			$('body').append('<div id="magic-dialog" title="Machine Learning Dialog" style="display:none; z-index:999; "></div>');
		}
		$tenantId = this.config.tenantId;
		$dialog = $("#magic-dialog");
		$dialog.dialog({
			minWidth: 600,
			minHeight: 600,
			height: 600,
			autoOpen: false
		});
		
		$dialog.html("");
		
		fromKid = data.fromKid;
		toKid = data.toKid;
		mergeTestCases = data.mergeTestCases;
		mergeInheritantTemplates = data.mergeInheritantTemplates;

		$title = "知識點自動合併";
		$buf = "";
		$buf += "<h4 class='alert alert-danger'>知識點合併注意事項</h4>";
		$buf += "<h4 class='alert alert-warning'><ul class='unstyled-list'>";
		$buf += "<li>被合併的知識點將被「停用」且所有「斷句、例句跟繼承範本」都會合併到存續知識點當中</li>";
		$buf += "<li>若有重複的會被剔除，若繼承範本有衝突，以存續的知識點為主</li>";
		$buf += "<li>答案將不會被合併，僅保留存續知識點的答案</li>"
		$buf += "<li>測試紀錄的預期正確答案編號將會被更新為存續知識點的編號</li>"
		$buf += "<li>被消滅的知識點的「標準問」將自動加入存續知識點的例句（不進行斷句整句放入）當中</li>"
		$buf += "</ul></h4>";

		$buf += "<h4>即將開始自動將知識點「" + data.fromKid + "」合併至知識點「" + data.toKid + "」<br>";
		$buf += "時間可能要幾秒鐘到數分鐘不等（主要看問法數量及測試紀錄筆數）<br>";
		$buf += "按下Ok開始進行...<br></h4>";
		$buf = $($buf);

		myConfirm($title, $buf, function() {
			$thisModal = $buf;
			callback();
		});
	};

	QAUtility.prototype.stop = function() {
	};

	// 不使用 `new` 來呼叫建構函式的話，你可能會這樣做：
	var constructor = function(config) {
		return new QAUtility(config);
	};

	// 把你的模組變成全域物件
	global.createQAUtility = constructor;

})(this);