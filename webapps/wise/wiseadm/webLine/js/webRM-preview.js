(function($) {
	function createLineActionButton(action) {
		if (action.type == 'uri') {
			actionBtn = $('<li />');
			actionUri = $('<a/>').attr('href', action.uri).attr('target', '_blank').appendTo(actionBtn);
			actionUri.text(action.label);
		}
		else if (action.type == 'postback') {
			actionBtn = $('<li class="question-clickable" />');
			actionBtn.data('postback', action.data);
			actionBtn.text(action.label);
		}
		else {
			actionBtn = $('<li class="question-clickable" />');
			actionBtn.data('question', action.text);
			actionBtn.text(action.label);
		}
		
		if (typeof action.es !== 'undefined') actionBtn.data('es', action.es);
		if (typeof action.est !== 'undefined') actionBtn.data('est', action.est); 
		
		return actionBtn;
	}
	
	function createLineQuickRepliesButton(item) {
		if (item.type == 'action') {
			if (item.action.type == 'message') {
				actionBtn = $("<li class='form-control form-control-round question-clickable' />");
				actionBtn.data('question', item.action.text);
				actionBtn.text(item.action.label);
			}
			else if (item.action.type == 'postback') {
				actionBtn = $("<li class='form-control form-control-round question-clickable' />");
				actionBtn.data('postback', item.action.data);
				actionBtn.text(item.action.label);
			}
			// 其他特定功能就不要讓他可以點擊
			else {
				actionBtn = $("<li class='form-control form-control-round' />");
				actionBtn.text(item.action.label + '(' + item.action.type + ')');
			}
		}
		
		if (typeof item.action.es !== 'undefined') actionBtn.data('es', item.action.es);
		if (typeof item.action.est !== 'undefined') actionBtn.data('est', item.action.est); 
		
		return actionBtn;
	}
	
	function createLineImagemapButton(action, scale) {
		var area = action.area;
		var x1 = area.x / scale;
		var y1 = area.y / scale;
		var x2 = (area.x + area.width) / scale;
		var y2 = (area.y + area.height) / scale
		if (action.type == 'message') {
			actionBtn = $('<area shape="rect" coords="' + x1 +  ',' + y1 +  ',' + x2 +  ',' + y2 +
				'" class="question-clickable" data-opnum="' + action.text + '">');
		}
		else if (action.type == 'uri') {
			actionBtn = $('<area shape="rect" coords="' + x1 +  ',' + y1 +  ',' + x2 +  ',' + y2 +
				'" href="' + action.linkUri + '" target="_blank">');
		}
		
		return actionBtn;
	}
	
	function createMessengerQuickRepliesButton(action) {
		if (action.content_type == 'text') {
			actionBtn = $("<li class='form-control form-control-round question-clickable' />");
			actionBtn.data('question', action.payload);
			actionBtn.text(action.title);
		}
		// 其他特定功能就不要讓他可以點擊，可以出現content_type就好
		else {
			actionBtn = $("<li class='form-control form-control-round' />");
			actionBtn.data('postback', action.payload);
			actionBtn.text(action.content_type);
		}
		
		if (typeof action.es !== 'undefined') actionBtn.data('es', action.es);
		if (typeof action.est !== 'undefined') actionBtn.data('est', action.est); 
		
		return actionBtn;
	}
	
	function createMessengerActionButton(action) {
		if (action.type == 'web_url') {
			actionBtn = $('<li />');
			actionUri = $('<a/>').attr('href', action.url).attr('target', '_blank').appendTo(actionBtn);
			actionUri.text(action.title);
		}
		else if (action.type == 'phone_number') {
			actionBtn = $('<li />');
			actionUri = $('<a/>').attr('href', 'tel:' + action.payload).attr('target', '_blank').appendTo(actionBtn);
			actionUri.text(action.title);
		}
		else if (action.type == 'postback') {
			actionBtn = $('<li class="question-clickable" />');
			actionBtn.data('postback', action.payload);
			actionBtn.text(action.title);
		}
		else {
			actionBtn = $('<li class="question-clickable" />');
			actionBtn.data('question', action.payload);
			actionBtn.text(action.title);
		}
		
		if (typeof action.es !== 'undefined') actionBtn.data('es', action.es);
		if (typeof action.est !== 'undefined') actionBtn.data('est', action.est); 
		
		return actionBtn;
	}
	
	function createActionsOnGoogleQuickRepliesButton(action) {
		if (typeof(action.title) != 'undefined') {
			actionBtn = $("<li class='form-control form-control-round question-clickable' />");
			actionBtn.data('question', action.title);
			actionBtn.text(action.title);
		}
		// 其他特定功能就不要讓他可以點擊，可以出現content_type就好
		else if (typeof(action.destinationName) != 'undefined') {			
			actionBtn = $("<li class='form-control form-control-round' />");
			// 2019/06/19 url deprecated
			url = action.url != null ? action.url : action.openUrlAction.url;
			actionUri = $('<a/>').attr('href', url).attr('target', '_blank').appendTo(actionBtn);
			actionUri.text(action.destinationName);
		}
		
		if (typeof action.es !== 'undefined') actionBtn.data('es', action.es);
		if (typeof action.est !== 'undefined') actionBtn.data('est', action.est); 
		
		return actionBtn;
	}
	
	function createActionsOnGoogleActionButton(action, msgType) {
		if (typeof(action.optionInfo) != 'undefined') {
			if (msgType == 'listSelect') {
				actionBtn = $('<li class="question-clickable" style="text-align:left;margin-left:10px;height:100%;line-height:25px" />');
				actionBtn.data('postback', action.optionInfo.key);
				actionBtn.text(action.title);
				if (action.hasOwnProperty('description')) {
					actionBtn.html('<h4>' + actionBtn.html() + '</h4>' + action.description.replace(/\n/g, "<br>"));
				}
			} 
			else if (msgType == 'carouselSelect') {
				actionBtn = $('<li class="question-clickable" />');
				actionBtn.data('postback', action.optionInfo.key);
				actionBtn.text(action.title);
			}
		}
		
		if (typeof(action.openUrlAction) != 'undefined') {
			if (msgType == 'basicCard') {
				actionBtn = $('<li/>');
				actionUri = $('<a/>').attr('href', action.openUrlAction.url).attr('target', '_blank').appendTo(actionBtn);
				actionUri.text(action.title);
			} 
			else if (msgType == 'carouselBrowse') {
				actionBtn = $('<li style="text-align:left;margin-left:10px;height:100%;line-height:30px"/>');
				actionUri = $('<a/>').attr('href', action.openUrlAction.url).attr('target', '_blank').appendTo(actionBtn);
				actionUri.text(action.title);
				if (action.hasOwnProperty('description')) {
					actionUri.html('<h4>' + actionUri.html() + '</h4>' + action.description.replace(/\n/g, "<br>"));
				}
				if (action.hasOwnProperty('footer')) {
					actionUri.html(actionUri.html() + '<footer style="font-size:10pt">' + action.footer.replace(/\n/g, "<br>") + '</footer>');
				}
				
			}
		}
		return actionBtn;
	}
	
	function createColumnDivByActionsOnGoogle(column) {
		msgBlock = $('<div class="buttons-message-block"></div>');
		if (column.hasOwnProperty("image")) {
			imgBlock = $('<div class="img-wrapper" />').appendTo(msgBlock);
			$('<img />').attr('src', column.image.url).appendTo(imgBlock);
		}
		descBlock = $('<div class="description" />').appendTo(msgBlock);
		if (column.title) {
			$('<h4/>').html(column.title).appendTo(descBlock);
		}
		if (column.description) {
			descBlock.html(descBlock.html() + column.description.replace(/\n/g, "<br>"));
		}
		actions = $('<ul class="options" />').appendTo(msgBlock);
		createActionsOnGoogleActionButton(column, 'carouselSelect').appendTo(actions);
		return msgBlock;
	}
	
	// LINE Carousel Column
	function createColumnDiv(column) {
		msgBlock = $('<div class="buttons-message-block"></div>');
		
		if (column.hasOwnProperty("thumbnailImageUrl") && column.thumbnailImageUrl != null) {
			imgBlock = $('<div class="img-wrapper" />').appendTo(msgBlock);
			$('<img />').attr('src', column.thumbnailImageUrl).appendTo(imgBlock);
		}
		descBlock = $('<div class="description" />').appendTo(msgBlock);
		
		if (column.title) {
			$('<h4/>').html(column.title).appendTo(descBlock);
		}
		if (column.text) {
			descBlock.html( descBlock.html() + column.text.replace(/\n/g, "<br>") );
		}
		
		actions = $('<ul class="options" />').appendTo(msgBlock);
		
		for (var ac=0; ac < column.actions.length; ac++) {
			action = column.actions[ac];
			createLineActionButton(action).appendTo(actions);
		}
		
		return msgBlock;
	}
	
	// Messenger Generic Element
	function createElementDiv(element) {
		msgBlock = $('<div class="buttons-message-block"></div>');
		
		if (element.hasOwnProperty("image_url") && element.image_url != null) {
			imgBlock = $('<div class="img-wrapper" />').appendTo(msgBlock);
			$('<img />').attr('src', element.image_url).appendTo(imgBlock);
		}
		descBlock = $('<div class="description" />').appendTo(msgBlock);
		
		if (element.title) {
			$('<h4/>').html(element.title).appendTo(descBlock);
		}
		if (element.subtitle) {
			descBlock.html( descBlock.html() + element.subtitle.replace(/\n/g, "<br>") );
		}
		
		actions = $('<ul class="options" />').appendTo(msgBlock);
		
		for (var ac=0; ac < element.buttons.length; ac++) {
			action = element.buttons[ac];
			createMessengerActionButton(action).appendTo(actions);
		}
		
		return msgBlock;
	}

    $.fn.appendRichMessage = function(msg) {
    	this.each (function() {
    		$this = $(this);
    		if (msg != null) {
				if (msg.type == 'text' || typeof(msg.text) != 'undefined') {
					$this.find('.msg-content').addClass('msg').html(msg.text.replace(/\n/g, "<br>"));
				}
				else if (msg.type == 'html') {
					$this.find('.msg-content').html(msg.html);
				}
				else if (msg.type == 'image') {
					msgDiv = $('<div class="buttons-message-container" style="width:500px !important"></div>').appendTo($this.find('.msg-content'));
					msgBlock = $('<div class="buttons-message-block" style="width:500px !important"></div>').appendTo(msgDiv);
					msgImage = $('<img src="' + msg.originalContentUrl + '" width="460" height="auto"></img>').appendTo(msgBlock);
				}
				else if (msg.type == 'imagemap') {
					tmpl = msg;
					msgDiv = $('<div class="buttons-message-container" style="width:500px !important"></div>').appendTo($this.find('.msg-content'));
					msgBlock = $('<div class="buttons-message-block" style="width:500px !important"></div>').appendTo(msgDiv);
					msgImage = $('<img src="' + msg.baseUrl + '/460' + '" class="imagemap" usemap="#' + msg.altText + '" width="460" height="auto"></img>').appendTo(msgBlock);
	
	        		var scale = 2.26; // 1040 / 460
	
					msgMap = $('<map name="' + msg.altText + '"></map>').appendTo(msgImage);
					for (var ac=0; ac < tmpl.actions.length; ac++) {
						action = tmpl.actions[ac];
						createLineImagemapButton(action, scale).appendTo(msgMap);
					}
				}
				else if (msg.type == 'template' && msg.template.type == 'buttons') {
					tmpl = msg.template;
					msgDiv = $('<div class="buttons-message-container"></div>').appendTo($this.find('.msg-content'));
					msgBlock = $('<div class="buttons-message-block"></div>').appendTo(msgDiv);
						if (tmpl.hasOwnProperty("thumbnailImageUrl") && tmpl.thumbnailImageUrl != null) {
						imgBlock = $('<div class="img-wrapper" />').appendTo(msgBlock);
						$('<img />').attr('src', tmpl.thumbnailImageUrl).appendTo(imgBlock);
					}
					descBlock = $('<div class="description" />').appendTo(msgBlock);
			
					if (tmpl.title) {
						$('<h4/>').html(tmpl.title).appendTo(descBlock);
					}
					if (tmpl.text) {
						descBlock.html( descBlock.html() + tmpl.text.replace(/\n/g, "<br>") );
					}
					
					actions = $('<ul style="margin-top: 5px;" />').appendTo(msgBlock);
					
					for (var ac=0; ac < tmpl.actions.length; ac++) {
						action = tmpl.actions[ac];
						createLineActionButton(action).appendTo(actions);
					}
				}
				else if (msg.type == 'template' && msg.template.type == 'carousel') {
					tmpl = msg.template;
					$this.addClass('swiper');
					$this.prepend('<div class="swiper-control-button prev"></div>');
					$this.prepend('<div class="swiper-control-button next"></div>');
					msgDiv = $('<div class="swiper-container" />').appendTo($this.find('.msg-content').addClass('swiper-scroll-container'));
					columnsDiv = $('<div class="buttons-message-container"></div>').appendTo(msgDiv);
					
					for (var ci=0; ci < msg.template.columns.length; ci++) {
						column = msg.template.columns[ci];
						createColumnDiv(column).appendTo(columnsDiv);
					}
				}
				// Messenger Template的格式
				else if (typeof(msg.attachment) != 'undefined') {
					tmpl = msg.attachment.payload;
	    			if (msg.attachment.type == 'template') {
	    				if (msg.attachment.payload.template_type == 'button') {
	    					msgDiv = $('<div class="buttons-message-container"></div>').appendTo($this.find('.msg-content'));
	    					msgBlock = $('<div class="buttons-message-block"></div>').appendTo(msgDiv);
	    					msgBlock.html($.trim(tmpl.text.replace(/\n/g, "<br>")));
	    					
	    					actions = $('<ul style="margin-top: 5px;" />').appendTo(msgBlock);
	    					
	    					for (var ac=0; ac < tmpl.buttons.length; ac++) {
	    						action = tmpl.buttons[ac];
	    						createMessengerActionButton(action).appendTo(actions);
	    					}
	    				}
	    				else if (msg.attachment.payload.template_type == 'generic') {
	    					$this.addClass('swiper');
	    					$this.prepend('<div class="swiper-control-button prev"></div>');
	    					$this.prepend('<div class="swiper-control-button next"></div>');
	    					msgDiv = $('<div class="swiper-container" />').appendTo($this.find('.msg-content').addClass('swiper-scroll-container'));
	    					elementsDiv = $('<div class="buttons-message-container"></div>').appendTo(msgDiv);
	    					
	    					for (var ci=0; ci < tmpl.elements.length; ci++) {
	    						element = tmpl.elements[ci];
	    						createElementDiv(element).appendTo(elementsDiv);
	    					}
	    				}
	    			}
	    			// 一般圖片
	    			else if (msg.attachment.type == 'image') {
	    				msgDiv = $('<div class="buttons-message-container" style="width:500px !important"></div>').appendTo($this.find('.msg-content'));
						msgBlock = $('<div class="buttons-message-block" style="width:500px !important"></div>').appendTo(msgDiv);
						msgImage = $('<img src="' + tmpl.url + '" width="460" height="auto"></img>').appendTo(msgBlock);
	    			}
	    		} 
				// Actions On Google Text
				else if (typeof(msg.simpleResponse) != 'undefined') {
					var text = msg.simpleResponse.textToSpeech;
					if (typeof(msg.simpleResponse.displayText) != 'undefined') {
						text = msg.simpleResponse.displayText;
					}
					$this.find('.msg-content').addClass('msg').html(text.replace(/\n/g, "<br>"));
				}
				// Actions On Google ListSelect or CarouselSelect
				else if (typeof(msg.inputValueData) != 'undefined') {
					if (typeof(msg.inputValueData.listSelect) != 'undefined') {
						var tmpl = msg.inputValueData.listSelect;
						msgDiv = $('<div class="buttons-message-container"></div>').appendTo($this.find('.msg-content'));
						msgBlock = $('<div class="buttons-message-block"></div>').appendTo(msgDiv);
						
						descBlock = $('<div class="description" />').appendTo(msgBlock);
				
						if (tmpl.title) {
							$('<h4/>').html(tmpl.title).appendTo(descBlock);
						}
						if (tmpl.description) {
							descBlock.html( descBlock.html() + tmpl.description.replace(/\n/g, "<br>") );
						}
						
						actions = $('<ul/>').appendTo(msgBlock);
						
						for (var ac=0; ac < tmpl.items.length; ac++) {
							action = tmpl.items[ac];
							createActionsOnGoogleActionButton(action, 'listSelect').appendTo(actions);
						}
					}
					else if (typeof(msg.inputValueData.carouselSelect) != 'undefined') {
						var tmpl = msg.inputValueData.carouselSelect;
						$this.addClass('swiper');
						$this.prepend('<div class="swiper-control-button prev"></div>');
						$this.prepend('<div class="swiper-control-button next"></div>');
						msgDiv = $('<div class="swiper-container" />').appendTo($this.find('.msg-content').addClass('swiper-scroll-container'));
						columnsDiv = $('<div class="buttons-message-container"></div>').appendTo(msgDiv);
						for (var ci=0; ci < tmpl.items.length; ci++) {
							column = tmpl.items[ci];
							createColumnDivByActionsOnGoogle(column).appendTo(columnsDiv);
						}
					}
				}
				else if (typeof(msg.basicCard) != 'undefined') {
					var tmpl = msg.basicCard;
					msgDiv = $('<div class="buttons-message-container"></div>').appendTo($this.find('.msg-content'));
					msgBlock = $('<div class="buttons-message-block"></div>').appendTo(msgDiv);
					if (tmpl.hasOwnProperty("image")) {
						imgBlock = $('<div class="img-wrapper" />').appendTo(msgBlock);
						$('<img />').attr('src', tmpl.image.url).appendTo(imgBlock);
					}
					descBlock = $('<div class="description" />').appendTo(msgBlock);
					if (tmpl.title) {
						$('<h4/>').html(tmpl.title).appendTo(descBlock);
					}
					if (tmpl.subtitle) {
						descBlock.html( descBlock.html() + tmpl.subtitle.replace(/\n/g, "<br>"));
					}
					if (tmpl.formattedText) {
						descBlock.html( descBlock.html() + "<br>" + tmpl.formattedText.replace(/\n/g, "<br>"));
					}
					actions = $('<ul/>').appendTo(msgBlock);
					for (var ac=0; ac < tmpl.buttons.length; ac++) {
						action = tmpl.buttons[ac];
						createActionsOnGoogleActionButton(action, 'basicCard').appendTo(actions);
					}
				}
				else if (typeof(msg.carouselBrowse) != 'undefined') {
					var tmpl = msg.carouselBrowse;
					msgDiv = $('<div class="buttons-message-container"></div>').appendTo($this.find('.msg-content'));
					msgBlock = $('<div class="buttons-message-block"></div>').appendTo(msgDiv);
					actions = $('<ul/>').appendTo(msgBlock);
					for (var ac=0; ac < tmpl.items.length; ac++) {
						action = tmpl.items[ac];
						createActionsOnGoogleActionButton(action, 'carouselBrowse').appendTo(actions);
					}
				}
				
				if (typeof(msg.quick_replies) != 'undefined') {
					tmpl = msg.quick_replies;
					msgDiv = $('<div class="swiper-container" style="clear: both;" />').appendTo($this.find('.msg-content').addClass('swiper-scroll-container'));
					msgContainer = $('<div class="quickreplies-container"></div>').appendTo(msgDiv);
					msgBlock = $('<div class="quickreplies-message-block"></div>').appendTo(msgContainer);
					actions = $('<ul />').appendTo(msgBlock);
					for (var ac=0; ac < tmpl.length; ac++) {
						action = tmpl[ac];
						createMessengerQuickRepliesButton(action).appendTo(actions);
					}
				}
				else if (typeof(msg.quickReply) != 'undefined') {
					tmpl = msg.quickReply;
					msgDiv = $('<div class="swiper-container" style="clear: both;"  />').appendTo($this.find('.msg-content').addClass('swiper-scroll-container'));
					msgContainer = $('<div class="quickreplies-container"></div>').appendTo(msgDiv);
					msgBlock = $('<div class="quickreplies-message-block"></div>').appendTo(msgContainer);
					actions = $('<ul />').appendTo(msgBlock);
		
					for (var ac=0; ac < tmpl.items.length; ac++) {
						action = tmpl.items[ac];
						createLineQuickRepliesButton(action).appendTo(actions);
					}
				}
				else if (typeof(msg.suggestions) != 'undefined') {
					items = msg.suggestions;
					msgDiv = $('<div class="swiper-container" style="clear: both;"  />').appendTo($this.find('.msg-content').addClass('swiper-scroll-container'));
					msgContainer = $('<div class="quickreplies-container"></div>').appendTo(msgDiv);
					msgBlock = $('<div class="quickreplies-message-block"></div>').appendTo(msgContainer);
					actions = $('<ul />').appendTo(msgBlock);
					for (var ac=0; ac < items.length; ac++) {
						action = items[ac];
						createActionsOnGoogleQuickRepliesButton(action).appendTo(actions);
					}
				}
    		}
    	});
    }

}(jQuery));
