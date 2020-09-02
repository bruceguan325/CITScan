
options.afterSearchCallback = function(result) {
	requirejs(['d3'], function() {
		requirejs(['d3.layout.cloud', 
	         'wordcloud'], 
	         
			function() {
				for (var i=0; i < result.page.length; i++) {
					if (result.page[i].selected) {
						
						for (var j=0; j < result.page[i].facets.length; j++) {
							var f = result.page[i].facets[j].data;
							
							if (result.page[i].facets[j].name == '作物') {
								loadByKeywords(f);
								break;
							}
						}
						break;
					}
				}
			});
	});
};

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
			
			requirejs([options.baseUrl + "/script/ICanHaz.min.js"], function() {
				window.onpopstate = search.init;
				$(document).ready(search.init);
			});
		});