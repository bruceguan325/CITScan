(function($) {
	function createActionButton(action) {
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
			actionBtn.text(action.label);
		}
		
		if (typeof action.es !== 'undefined') actionBtn.data('es', action.es);
		if (typeof action.est !== 'undefined') actionBtn.data('est', action.est); 
		
		return actionBtn;
	}
	
	function createColumnDiv(column) {
		msgBlock = $('<div class="buttons-message-block"></div>');
		
		if (column.hasOwnProperty("thumbnailImageUrl")) {
			imgBlock = $('<div class="img-wrapper" />').appendTo(msgBlock);
			$('<img />').attr('src', column.thumbnailImageUrl).appendTo(imgBlock);
		}
		descBlock = $('<div class="description" />').appendTo(msgBlock);
		
		if (column.title) {
			$('<h4/>').text(column.title).appendTo(descBlock);
		}
		if (column.text) {
			descBlock.html( descBlock.html() + column.text );
		}
		
		actions = $('<ul class="options" />').appendTo(msgBlock);
		
		for (var ac=0; ac < column.actions.length; ac++) {
			action = column.actions[ac];
			createActionButton(action).appendTo(actions);
		}
		
		return msgBlock;
	}

    $.fn.appendRichMessage = function(msg) {
    	this.each (function() {
    		$this = $(this);

			if (msg.type == 'text') {
				$this.find('.msg-content').addClass('msg').text(msg.text);
			}
			else if (msg.type == 'html') {
				$this.find('.msg-content').addClass('msg').html(msg.html);
			}
			else if (msg.type == 'template' && msg.template.type == 'buttons') {
				tmpl = msg.template;
				msgDiv = $('<div class="buttons-message-container"></div>').appendTo($this.find('.msg-content'));
				msgBlock = $('<div class="buttons-message-block"></div>').appendTo(msgDiv);
				msgBlock.html($.trim(tmpl.title) + '<br>' + $.trim(tmpl.text));
				
				actions = $('<ul style="margin-top: 5px;" />').appendTo(msgBlock);
				
				for (var ac=0; ac < tmpl.actions.length; ac++) {
					action = tmpl.actions[ac];
					createActionButton(action).appendTo(actions);
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
    	});
    }

}(jQuery));