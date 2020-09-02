var currDsId = 0;
var currLink = "";
var queryLock = false;

String.prototype.format = function() {
	  var args = arguments;
	  return this.replace(/{(\d+)}/g, function(match, number) { 
	    return typeof args[number] != 'undefined'
	      ? args[number]
	      : match
	    ;
	  });
	};
	
var WiSe = function(custom_options) {
	var options = {
			d: 0,
			baseUrl: '/',
			maxDescriptionLength: 200,
			beforeSearchCallback: function() {},
			afterSearchCallback: function() {},
			beforeRenderCallback: function(data) {},
	};
	$.extend(options, custom_options);
	if (currDsId == 0) {
		currDsId = options.d;
	}
	var lastHash = "##";
	var parseQueryMap = function(query) {
		  var data = query.split("&");
		  var result = {};
		  for(var i=0; i<data.length; i++) {
		    var item = data[i].split("=");
		    result[item[0]] = item[1];
		  }
		  return result;
	}
	
	
	var keyPressed = false;
	var renderSearchBox = function(query, hotkeywords) {
		$('#searchBox').html(ich.searchBoxTemplate({q: query, hotKeywords: hotkeywords}));
		$('#searchText').typeahead({
			  highlight: true,
			  hint: false
			}, {
  			  name: 'hotkeywords',
			  source: getSuggest,
			  limit: 10
			}
			);

		$("#searchText").keyup(function(event) {
			var currText = $('#searchText').val();
		    if (event.keyCode == 13 && keyPressed) {
		    	event.preventDefault();
		    	doSearch(currText);
		    }
		    keyPressed = false;
		});
		$("#searchText").keypress(function(event) { 
			if (event.which == 13) {
				keyPressed = true;
			}
		});
	}
	
	var renderTabs = function(pages) {
		$('#tabs').html(ich.tabsTemplate({tabs:pages}));
	}
	var renderResult = function(pageObj) {
		var start = parseInt(pageObj.query.start,10);
		var rows = parseInt(pageObj.query.rows,10);
		var total = pageObj.numFound;
		var maxPage = Math.floor((total-1) / rows) + 1;
		
		var currPage = Math.floor( start / rows ) + 1;
		var startPage = Math.max(1, currPage - 4);
		var endPage = Math.min(maxPage, currPage + 4);
		
		currLink = pageObj.query.link;
	
		// 分頁功能
		var pages = [];
		for (var i=startPage; i <= endPage; i++) {
			var page = {
					active: (i == currPage)
			};
			page.pageNo = i;
			pages.push(page);
		}
		
		var paginationData = {
			rows: rows,
			pages: pages,	
		}
		if (startPage != 1) {
			paginationData.firstPage = 1;
		}
		if (endPage != maxPage) {
			paginationData.lastPage = maxPage;
		}
		var pagination = ich.paginationTemplate(paginationData);
		$('.search-pagination').html( pagination );
		
		var info = {
				qTime: pageObj.qTime,
				numFound: total,
				startPlusOne: start + 1,
				startPlusRows: start + rows,
		}
		
		if (typeof(pageObj.queryHistories) != 'undefined') {
			if ( ich.hasOwnProperty('queryHistoriesTemplate') ) { // for backward compatibility
				$('.query-histories').html( ich.queryHistoriesTemplate({histories: pageObj.queryHistories}) );
				$('.query-histories').show();
			}
		}
		else {
			$('.query-histories').hide();
		}
		
		$('#info').html( ich.infoTemplate({info:info, sortables:pageObj.sortables}) );
		
		// 排序區塊
		//$('#sortables').html("");
		var sortables = pageObj.sortables;
		for (var i=0; i < sortables.length; i++) {
			var s = sortables[i];
			
			if (typeof(s.order) != 'undefined') {
				if (s.order == 'asc') {
					$('#currentSort').html(s.name + ' <i class="glyphicon glyphicon-sort-by-attributes"></i> 升冪');
				}			
				else {
					$('#currentSort').html(s.name + ' <i class="glyphicon glyphicon-sort-by-attributes-alt"></i> 降冪');
				}
			}
			
		}
		
		// 搜尋結果清單
		var docs = pageObj.docs ? pageObj.docs : [];
		$.each(docs, function(i, doc) {
			var desc = [];
			doc.Description_mt = desc;
			for (var i=0; i < pageObj.bodyFields.length; i++) {
				var fn = pageObj.bodyFields[i];
				if (typeof(doc[fn]) != 'undefined') {
					var data = doc[fn];
					if (data.length > options.maxDescriptionLength) {
						data = data.substring(0, options.maxDescriptionLength) + "...";
					}
					desc.push(data);
				}
			}
		});
		var toLive = ich.docTemplate({docs:docs});
		$('#results').html(toLive);
		toLive.fadeIn(800);
		
		// 多維度
		$('#facet-fields').html(ich.facetTemplate({facets: pageObj.facets}));
	}
	
	var doSearch = function(query) {
		if (queryLock) 
			return;
		
		queryLock = true;
		$('.loading-panel').show();
		if (query == '*:*') {
			window.location.hash = '';
			document.title = 'WiSe - 智慧型搜尋引擎';
		}
		else {
			window.location.hash = '#' + query;
			document.title = query + ' - WiSe - 智慧型搜尋引擎';
		}
		options.beforeSearchCallback();
		
		$.ajax({
			url: options.baseUrl + "/query",
			data: {q: query, d: currDsId, format: "json"},
			dataType: "jsonp",
			success: function(result) {
				setTimeout(function() {
				queryLock = false;

				options.beforeRenderCallback(result);
				renderTabs(result.page);
				
				for (var i=0; i < result.page.length; i++) {
					if (result.page[i].selected) {
						currDsId = result.page[i].id;
						renderResult(result.page[i]);
						break;
					}
				}
				$('.loading-panel').fadeOut(300);

				options.afterSearchCallback(result);
				}, 200);
			}
		});	
	}
	var doSearchWithGetQuery = function(query) {
		if (queryLock) 
			return;
		
		queryLock = true;
		$('.loading-panel').show();
		window.location.hash = '#' + query;
		options.beforeSearchCallback();
		
		$.ajax({
			url: options.baseUrl + "/query?format=json&" + query,
			dataType: "jsonp",
			success: function(result) {
				setTimeout(function() {
				queryLock = false;

				options.beforeRenderCallback(result);
				renderTabs(result.page);
				
				for (var i=0; i < result.page.length; i++) {
					if (result.page[i].selected) {
						currDsId = result.page[i].id;
						renderResult(result.page[i]);
						break;
					}
				}
				$('.loading-panel').fadeOut(500);
				options.afterSearchCallback(result);
				}, 300);
			}
		});	
	}
	
	var getSuggest = function(query, callback) {
		$.ajax({
			url: options.baseUrl + "/getSuggestKeyword",
			data: {q: query},
			dataType: "jsonp",
			success: function(result) {
				callback($.map(result, function(cell) { return { value: cell }; }));
			}});
	};
	
	var init = function () { 
		var hash = window.location.hash;
		
		if (hash == lastHash)
			return;
		
		lastHash = hash;
		if (typeof(hash) == 'undefined' || hash == '') {
			doSearch("*:*");
			
	
			$.ajax({
				url: options.baseUrl + "/getHotKeyword",
				dataType: "jsonp",
				success: function(result) {
					renderSearchBox("", result);
				},
				error: function() {
					renderSearchBox("", []);
				}
			});
		}
		else {
			var query = hash.substring(1);
			
			if (query.indexOf('&') != -1) {
				doSearchWithGetQuery(query);
				
				var queryJson = parseQueryMap(query);
				query = queryJson.q;			
			}
			else {
				doSearch(query);
			}
			
	
			$.ajax({
				url: options.baseUrl + "/getHotKeyword",
				dataType: "jsonp",
				success: function(result) {
					renderSearchBox(decodeURIComponent(query), result);
				},
				error: function() {
					renderSearchBox(decodeURIComponent(query), []);
				}
			});
		}
	}
	
	$('.page-btn').live('click', function(event) {
		event.preventDefault();
		var gotoPage = parseInt($(this).attr('data-page'));
		var rows = parseInt($(this).attr('data-rows'));
		var start = (gotoPage - 1) * rows;
		var query = currLink + "&start=" + start + "&rows=" + rows;
		
		doSearchWithGetQuery(query);
	});
	
	$('#searchBtn').live('click', function(event) {
		event.preventDefault();
		doSearch($('#searchText').val());
	});
	
	$('.facet > a,#sortables a,#tabs a,.query-histories a[data-query]').live('click', function(event) {
		event.preventDefault();
		var query = $(this).attr('data-query');
		doSearchWithGetQuery(query);
	});
	
	$('.hot-keywords').live('click', function(event) {
		event.preventDefault();
		var kw = $(this).attr('data-query');
		doSearch(kw);
	});

	return {
		options: options,
		renderTabs: renderTabs,
		renderResult: renderResult,
		doSearch: doSearch,
		doSearchWithGetQuery: doSearchWithGetQuery,
		getSuggest: getSuggest,
		init: init,
	};
};

