
// http://jsfromhell.com/array/shuffle
Array.prototype.shuffle = function () { //v1.0
	for(var j, x, i = this.length; i; j = parseInt(Math.random() * i), x = this[--i], this[i] = this[j], this[j] = x);
	return this;
};

var isIE = navigator.appVersion.indexOf("MSIE") != -1;

function buildUrl(url, lang, cl, kw) {
	var tmp = url + '/' + lang + '/';
	if (typeof(kw) != 'undefined' && kw != '') {
		tmp += kw + '/'; 
	}

	if (typeof(cl) != 'undefined' && cl != '') {
		tmp += '?cl=' + cl; 
	}
	
	return tmp;
}

function isScrolledIntoView(elem, fullvisible)
{
    var docViewTop = $(window).scrollTop();
    var docViewBottom = docViewTop + $(window).height();

    var elemTop = $(elem).offset().top;
    var elemBottom = elemTop + $(elem).height();

    if (typeof(fullvisible) != 'undefined' && fullvisible) 
         return ((elemBottom >= docViewTop) && (elemTop <= docViewBottom) && (elemBottom <= docViewBottom) && (elemTop >= docViewTop));
    else
         return ((elemBottom >= docViewTop) && (elemTop <= docViewBottom));

}

// getPageScroll() by quirksmode.com
function getPageScroll() {
    var xScroll, yScroll;
    if (self.pageYOffset) {
      yScroll = self.pageYOffset;
      xScroll = self.pageXOffset;
    } else if (document.documentElement && document.documentElement.scrollTop) {
      yScroll = document.documentElement.scrollTop;
      xScroll = document.documentElement.scrollLeft;
    } else if (document.body) {// all other Explorers
      yScroll = document.body.scrollTop;
      xScroll = document.body.scrollLeft;
    }
    return new Array(xScroll,yScroll)
}

// Adapted from getPageSize() by quirksmode.com
function getPageHeight() {
    var windowHeight
    if (self.innerHeight) { // all except Explorer
      windowHeight = self.innerHeight;
    } else if (document.documentElement && document.documentElement.clientHeight) {
      windowHeight = document.documentElement.clientHeight;
    } else if (document.body) { // other Explorers
      windowHeight = document.body.clientHeight;
    }
    return windowHeight;
}


var setResizeHandler = function(handler) {
	var resizeDoIt;
	
	$(window).resize(function(){
		clearTimeout(resizeDoIt);
		resizeDoIt = setTimeout(function(){handler();}, 800);
	});
}