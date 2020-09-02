
(function(global) {

	function QABuildStatusChecker(data) {
		this.config = $.extend(data, this.config);
		return this;
	}

	QABuildStatusChecker.prototype.config = {
		docId : 0,
		enable : false
	};

	QABuildStatusChecker.prototype.start = function() {
		this.config.enable = true;
		this.checkStatus = (function(instance) {
			return function() {
				$.ajax({
					url : "qa-build-status-ajax.jsp?t=" + new Date().getTime(),
					dataType : "json",
					data : {
						docId : instance.config.docId
					},
					success : function(result) {
						console.log("statusUpdated:" + JSON.stringify(instance.config));
						instance.config.callback.statusUpdated.call(instance, result);
					}
				});
			};
		})(this);
		
		this.config.timer = setInterval(this.checkStatus, 1000);
	};

	QABuildStatusChecker.prototype.stop = function() {
		this.config.enable = false;
		if (typeof (this.config.timer) != 'undefined') {
			clearInterval(this.config.timer);
		}
		else {
			console.log("cannot clearTimer in srbt-qa-build-status");
		}
	};

	// 不使用 `new` 來呼叫建構函式的話，你可能會這樣做：
	var constructor = function(config) {
		return new QABuildStatusChecker(config);
	};

	// 把你的模組變成全域物件
	global.createQABuildStatusChecker = constructor;

})(this);