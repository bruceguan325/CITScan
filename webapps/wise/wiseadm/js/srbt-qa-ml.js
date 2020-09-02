
/**
 * 需要 JQuery UI Dialog
 * 需要 srbt-common.js
 * 同時需要 typed.js （呈現打字效果）
 * 
 * start 傳入的 data 為 json object 必需有 q 以及 correctKid 兩個 property
 */
(function(global) {

	function QALearningMachine(data) {
		this.config = $.extend(data, this.config);
		return this;
	}

	QALearningMachine.prototype.config = {
		docId : 0,
		enable : false
	};
	
	QALearningMachine.prototype.testQ = function(question, correctKid, stat) {
		return 
	};

	QALearningMachine.prototype.start = function(data, callback) {
		i18n.set({
			'lang':'srbt-qa-ml-' + this.config.language,
			'path':'/wise/wiseadm/js'
		});
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
		
		//console.log(data);
		var entryData = {};
		
		if (typeof(data['entryData']) != 'undefined') {
			entryData = data.entryData;
		}
		
		$buf = "";
		
		if (typeof(entryData['kps']) == 'undefined' || entryData.kps.length == 0) {
			$buf += "<h4 class='alert alert-danger'>" + i18n.t('there is no knowledge topic in this sentence') + "</h4>";
			
			if (typeof(entryData['lastKps']) != 'undefined' && entryData.lastKps.length > 0) {
				$buf += "<h4 class='alert alert-warning'>" + i18n.t('previous sentence reference knowledge plz consider') + "<br>";
				
				for (var kkk=0; kkk < entryData.lastKps.length; kkk++) {
					kp = entryData.lastKps[kkk];
					$buf += "" + kp.keyword + " (" + kp.category + ")： ";
					$buf += "<span class='text-danger'>" + kp.keyword + "" + data.q + "</span>";
					$buf += "<br>";
				}
				
				$buf += "</h4>";
			}
		}

		$title = i18n.t('machine learning');
		$buf += "<h4>" + i18n.t('this function will auto learn sentences') + "<br><textarea name='questions' class='text-danger' cols='50' rows='5'>" + data.q + "</textarea><br>";
		$buf += i18n.t('and check %1 whether can answer id', data.correctKid) + "<br>";
		$buf += i18n.t('if not will save') + "<br>";
		$buf += i18n.t('auto test once after learning') + "<br>";
		$buf += i18n.t('time duration depending on number of example sentences') + "<br>";
		$buf += "<span class='text-danger'>" + i18n.t('plz check the right answer below') + "</span><br>";
		$buf += i18n.t('right answer id') + ":<input type='text' name='correctAnswerId' value='"+data.correctKid+"' /><br>";
		$buf += i18n.t('will start to learn') + "<br>";
		$buf += i18n.t('press ok') + "...<br></h4>";
		$buf = $($buf);

		myConfirm($title, $buf, function() {
			$thisModal = $buf;
			$dialog.dialog('open');
			
			if ($thisModal.find('[name=correctAnswerId]').val()) {
				data.correctKid = $thisModal.find('[name=correctAnswerId]').val();
			}
			
			if ($thisModal.find('[name=questions]').val()) {
				text = $thisModal.find('[name=questions]').val();
				var lines = [];
			    $.each(text.split(/\n/), function(i, line){
			        if(line){
			            lines.push(line);
			        } else {
			            lines.push("");
			        }
			    });
			    data.q = lines;
			}
			
			$resultMsg = "";

			$("<h3></h3>").appendTo($dialog).typed({
		        strings: [i18n.t('now start to do nlp') + "... ^500"],
		        typeSpeed: 1,
		        onStringTyped: function() {
		    	    		$("<h3></h3>").appendTo($dialog).typed({
	    					strings: [i18n.t('%1 start to analysis question', data.correctKid) + "... ^500"],
	    					typeSpeed: 0,
					        onStringTyped: function() {
					        	if ($.isArray(data.q)) {
					        		var promises = [];
								var stat = {good: 0, bad: 0, unknown: 0};
					        		
								foreach = function(questions, idx) {
					        			var currQ = questions[idx];
					        			$.ajax({
							    			url: 'qa-nlp-test-ajax.jsp',
							    			dataType: 'json',
							    			data: {
							        		  q: currQ,
							        		  replaceSyn: true,
							        		  checkAndAddToKid: data.correctKid,
							        		  ts: Math.random()
							    			},
							    	    }).done(
								    	    	(function(idx) {
								    	    		return function(resp) {
								    	    			doNext = function() {
										    	    		if ((idx+1) < questions.length) {
										    	    		}
										    	    		else {
		      				    	    						callback(stat);
										    	    		}
								    	    			};
								    	    			
								    	    			if (resp.checkAndAddResult.status == "success") {
								    	    				$typingBlock = $("<h3 class='text text-success'></h3>");
								    	    				$typingBlock.appendTo($dialog).typed({
									    					strings: [i18n.t('confirm %1 there is no repeat sentences', resp.checkAndAddResult.originalQuestion) + "... <br> ^500 " + i18n.t('start to learn this sentence') + "<br> ^500 " + i18n.t('waiting for sentence finish to test') + "... ^100"],
									    					typeSpeed: 0,
													        onStringTyped: function() {
												    	    			var checker = createQABuildStatusChecker(
					    												{
					    													docId: 'COMMON_SENSE-' + data.correctKid, 
					    													label: 'singleChecker:' + resp.checkAndAddResult.originalQuestion,
					    													callback: {
					    														statusUpdated: function(result) {
				        										  					if (result.status == "DONE") {
					      				    	    										this.stop();
					      				    	    										
					      				    	    									/* Q test to check if it will return this answer correctly */
				        										  						p = $.ajax({
				        										  					   		url: '/wise/qa-ajax.jsp',
				        										  					   		dataType: 'json',
				        										  					   		data: {
				        										  					     		  q: resp.checkAndAddResult.originalQuestion,
				        										  					     		  testMode: true,
				        										  					     		  ftc: false,
				        										  					     		  tid: $tenantId,
				        										  					      		  html: true
				        										  					   		},
				        										  					   		success: function(testQresp) {
				        										  								var kid;
				        										  								if (testQresp.hasOwnProperty("kid")) {
				        										  									kid = testQresp.kid.toString();
				        										  								}
				        										  								
				        										  								if (isNaN(data.correctKid)){
				        										  									stat.unknown++;
				        										  								}
				        										  								else if (data.correctKid == kid) {
				        										  									stat.good++;
				        										  									$typingBlock.append('<h3 class="text text-primary">' + i18n.t('success') + '</h3>');
				        										  								}
				        										  								else {
				        										  									stat.bad++;
				        										  									
				        										  									$typingBlock.append('<h3 class="text text-danger">' + i18n.t('fail') + ''+(typeof(kid)!='undefined'?kid:i18n.t('no answer'))+'</h3>');
				        										  								}
					        										  							doNext();
				        										  					   		}
				        										  						});
					        										  				}
				        										  					else {
				        										  						// 沒有 DONE 就一直等到 DONE, 沒有判斷 status = FAILED 的例外
					    															}
						    													}
					    													}
					    												}
					    											);
							      				    	    			
							      				    	    			checker.start();
													        },
									        			});
										    	    	}
									    	    		else {
									    	    			if (resp.checkAndAddResult.status == "error") {
						    	    							stat.bad++;
										    	    			$("<h3 class='text text-danger'></h3>").appendTo($dialog).html(
										    					i18n.t('fail to save sentence %1', resp.checkAndAddResult.originalQuestion) + "... <br>"
										    					+ i18n.t('error message') + resp.checkAndAddResult.errorMsg + "<br>"
										    					+ (resp.checkAndAddResult.hasOwnProperty("foundInKid") 
										    					     ? (i18n.t('knowledge id') + " <a target='_new' href='qaDataEditor.jsp?id=COMMON_SENSE-" + resp.checkAndAddResult.foundInKid + "#sentences'>" + resp.checkAndAddResult.foundInKid + "</a><br>")
										    					     : "")
										    					 );
										    	    		}
										    	    		else {
										    	    			alert(JSON.stringify(resp));
										    	    		}
									    	    			
									    	    			doNext();
									    	    		}
								    	    		};
								    	    	})(idx)
							    	    		);
								};
								
								foreach(data.q, 0);
					        }
	    					},
	    				});
				}
	      });
		});
	};

	QALearningMachine.prototype.stop = function() {
	};

	// 不使用 `new` 來呼叫建構函式的話，你可能會這樣做：
	var constructor = function(config) {
		return new QALearningMachine(config);
	};

	// 把你的模組變成全域物件
	global.createQALearningMachine = constructor;

})(this);