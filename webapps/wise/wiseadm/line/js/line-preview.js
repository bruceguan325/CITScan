(function($) {
	function createActionButton(action) {
		actionBtn = $('<li class="question-clickable" />');
		actionBtn.text(action.label);
		
		return actionBtn;
	}
	
	function createColumnDiv(column) {
		msgBlock = $('<div class="line-buttons-message-block"></div>');
		
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

    $.fn.appendLineRichMessage = function(msg) {
    	this.each (function() {
    		$this = $(this);

			if (msg.type == 'text') {
				$('<div class="well"></div>').text(msg.text).appendTo($this.find('.msg-content'));
			}
			else if (msg.type == 'imagemap') {
				$this.find('.msg-content').addClass('msg').html(msg.html);
				imDiv = $('<div class="thumbnail" />').appendTo($this.find('.msg-content'));
				$('<img />').attr('src', msg.baseUrl + "/1040").appendTo(imDiv);
			}
			else if (msg.type == 'template' && msg.template.type == 'buttons') {
				tmpl = msg.template;
				msgDiv = $('<div class="line-buttons-message-container"></div>').appendTo($this.find('.msg-content'));
				msgBlock = $('<div class="line-buttons-message-block"></div>').appendTo(msgDiv);
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
				columnsDiv = $('<div class="line-buttons-message-container"></div>').appendTo(msgDiv);
				
				for (var ci=0; ci < msg.template.columns.length; ci++) {
					column = msg.template.columns[ci];
					createColumnDiv(column).appendTo(columnsDiv);
				}
			}
    	});
    }

}(jQuery));