
options.beforeRenderCallback = function(result) {
			for (var i=0; i < result.page.length; i++) {
				if (result.page[i].selected) {
					var newDocs = [];
					$.each(result.page[i].docs, function (i, doc) {
						if (i % 3 == 0) {
							if (i != 0) {
								newDocs.push({ IMNOTDOC:true, INSERTEND:true});
							}
							newDocs.push({ IMNOTDOC:true, INSERTBEGIN:true});
						}
						
						newDocs.push( doc );
					});
					newDocs.push({ IMNOTDOC:true, INSERTEND:true});
					
					result.page[i].docs = newDocs;
					break;
				}
			}
		};
var search = new WiSe(options);
require([options.baseUrl + "/script/text.js!layout.html",
         options.baseUrl + "/script/text.js!templates.html",
         options.baseUrl + "/script/text.js!styles.css"], 
         
         function (layout, templates, styles) {
			$('body').prepend(layout);
			$('#templateStyles').html(styles);
			$('#templateLoader').html(templates);
			
			require([options.baseUrl + "/script/ICanHaz.min.js"], function() {
				window.onpopstate = search.init;
				$(document).ready(search.init);
			});
		});