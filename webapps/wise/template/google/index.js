
var search = new WiSe(options);
require([options.baseUrl + "/script/text.js!layout.html",
         options.baseUrl + "/script/text.js!templates.html",
         options.baseUrl + "/script/text.js!styles.css"], 
         
         function (layout, templates, styles) {
			$('body').prepend(layout);
			
			// Hack for IE8
			if($.browser.msie) {
			    $("#templateStyles").prop('styleSheet').cssText=styles;
			} else {
				$('#templateStyles').append(styles);
			}
			$('#templateLoader').html(templates);
			
			require([options.baseUrl + "/script/ICanHaz.min.js"], function() {
				window.onpopstate = search.init;
				$(document).ready(search.init);
			});
		});