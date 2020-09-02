/**
 * 定義通用 script functions
 */
function genRichMessages(appendToHere, rmBaseDiv, resp) {
	var richMessage = 'undefined';
	if (typeof(resp.channelType) != 'undefined') {
		if (typeof(resp.rm) != 'undefined') {
			richMessage = resp.rm;
		}
		else {
			if (resp.channelType == 'PLAIN_TEXT' || resp.channelType == 'RICH_TEXT') {
				richMessage = typeof(resp.webRM) != 'undefined' ? resp.webRM : 'undefined';
			}
			else if (resp.channelType == 'LINE') {
				richMessage = typeof(resp.line) != 'undefined' ? resp.line : 'undefined';
			}
			else if (resp.channelType == 'FACEBOOK_MESSENGER') {
				richMessage = typeof(resp.messenger) != 'undefined' ? resp.messenger : 'undefined';
				console.log(richMessage);
			}
		}
	}
	
	if (richMessage == 'undefined') {
		return false;
    }
	
	for (var i = 0; i < richMessage.messages.length; i++) {
		if (richMessage.messages[i] == null) {
			continue;
		}

		var newRM;
    	if (typeof(rmBaseDiv) == 'function') {
    		newRM = rmBaseDiv();
    	}
    	else {
    		newRM = $(rmBaseDiv);
    		
    		if (i == 0) {
    			newRM.find('.body').html("");
    		}
    	}
    	newRM.appendRichMessage(richMessage.messages[i]);
    	
    	$("<div class='row'>&nbsp;</div><br>").appendTo(newRM.find('.body'));
        	
        if (typeof(richMessage.messages[i].quickReply) != 'undefined' ||
        	typeof(richMessage.messages[i].quick_replies) != 'undefined') {
        	newRM.find('.body').find('.quickreplies-container').prepend("<br>");
        }
    	
    	if (typeof(appendToHere) == 'function') {
    		appendToHere(newRM, i);
    	}
    	else {
    		newRM.appendTo(appendToHere);
    	}
	}
	
	return true;
}

/**
 * 用 Bootstrap modal 做 confirm
 */
var myConfirm = function(title, msg, okCallback) {
	if (!$('#dataConfirmModal').length) {
		$('body').append('<div id="dataConfirmModal" class="modal fade" tabindex="-1" role="dialog"> <div class="modal-dialog"> <div class="modal-content"> <div class="modal-header"> <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button> <h4 class="modal-title">Modal title</h4> </div> <div class="modal-body"> <p>One fine body&hellip;</p> </div> <div class="modal-footer"> <button type="button" class="btn btn-default" data-dismiss="modal">Close</button> <button type="button" class="btn btn-primary" id="dataConfirmOK" >Ok</button> </div> </div><!-- /.modal-content --> </div><!-- /.modal-dialog --> </div><!-- /.modal -->');
	}

	$('#dataConfirmModal').find('.modal-title').text(title);
	$('#dataConfirmModal').find('.modal-body').html(msg);
	$('#dataConfirmModal').modal({show:true});

	var onClick = function() {
		$('#dataConfirmModal').modal('toggle');
		okCallback();
	};

	$(document).on("click", '#dataConfirmOK', onClick);

	$('#dataConfirmModal').on('hidden.bs.modal', function () {
		$(document).off("click", '#dataConfirmOK', onClick);
	});

	return false;
};

/**
 * 取得現在網址上的某個 GET 參數
 */
var getUrlParameter = function getUrlParameter(sParam) {
    var sPageURL = window.location.search.substring(1),
        sURLVariables = sPageURL.split('&'),
        sParameterName,
        i;

    for (i = 0; i < sURLVariables.length; i++) {
        sParameterName = sURLVariables[i].split('=');

        if (sParameterName[0] === sParam) {
            return sParameterName[1] === undefined ? true : decodeURIComponent(sParameterName[1]);
        }
    }
};

/**
 * 取得並且變換網址上的某個 GET 參數，回傳完整的 URL
 */
var changeUrlParameter = function changeUrlParameter(sParam, sVal) {
	var base = window.location.origin + window.location.pathname;
	
    var sPageURL = window.location.search.substring(1),
        sURLVariables = sPageURL == "" ? [] : sPageURL.split('&'),
        sParameterName,
        i;
    
    var found = false;

    for (i = 0; i < sURLVariables.length; i++) {
        sParameterName = sURLVariables[i].split('=');
        
        if (i == 0) {
        	base += "?";
        }
        else {
        	base += "&";
        }

        if (sParameterName[0] === sParam) {
        	base += sParam + "=" + encodeURIComponent(sVal);
        	found = true;
        }
        else {
        	base += sParameterName[0] + "=" + sParameterName[1];
        }
    }
    
    if (!found) {
    	base += sURLVariables.length > 0 ? '&' : '?';
    	base += sParam + "=" + encodeURIComponent(sVal);
    }
    
    return base;
};