$(function () {
	
    $('#fileupload').fileupload({
        dataType: 'json',
        done: function (e, data) {
        	$("tr:has(td)").remove();
            $.each(data.result, function (index, file) {
            	
                $("#uploaded-files").append(
                		$('<tr/>')
                		.append($('<td/>').text(file.fileName))
                		.append($('<td/>').text(file.fileSize))
                		.append($('<td/>').text(file.fileType))
                		.append($('<td/>').text("@"+file.twitter))
                		.append($('<td/>').html(
                				"<a href='javascript:checkImportState(" + file.qaFile + ", " + index + ");'>Import</a>"
                			  + "&nbsp;<a href='/wise/wiseadm/fileUpload?c=d&f="+index+"'>Delete</a>"
                						))

                		)//end $("#uploaded-files").append()
            }); 
        },
        
        progressall: function (e, data) {
	        var progress = parseInt(data.loaded / data.total * 100, 10);
	        $('#progress .progress-bar').css(
	            'width',
	            progress + '%'
	        );
   		},
   		
		dropZone: $('#dropzone')
    }).bind('fileuploadsubmit', function (e, data) {
    });
   
});