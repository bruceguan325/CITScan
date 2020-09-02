var fill = d3.scale.category20b();

var w = $('#vis').width(),
    h = 200;

var words = [],
    max,
    scale = 1,
    complete = 0,
    keyword = "",
    tags,
    fontSize,
    maxLength = 30,
    fetcher = "http://api.hithot.cc/1/keywords?locale=zh_TW&cl=s&rt=a&f=jsonp",
    statusText = d3.select("#status");

var layout = d3.layout.cloud()
    .timeInterval(10)
    .size([w, h])
    .fontSize(function(d) { return d.size; })
    .text(function(d) { return d.text; })
    .on("word", progress)
    .on("end", draw);

var svg = d3.select("#vis").append("svg")
    .attr("width", w)
    .attr("height", h);

var background = svg.append("g"),
    vis = svg.append("g")
    .attr("transform", "translate(" + [w >> 1, h >> 1] + ")");

d3.select("#download-svg").on("click", downloadSVG);
d3.select("#download-png").on("click", downloadPNG);

var form = d3.select("#form")
    .on("submit", function() {
      search.doSearch(d3.select("#text").property("value"));
      d3.event.preventDefault();
    });
form.selectAll("input[type=number]")
    .on("click.refresh", function() {
      if (this.value === this.defaultValue) return;
      generate();
      this.defaultValue = this.value;
    });
form.selectAll("input[type=radio], #font")
    .on("change", generate);

function getURL(url) {
  statusText.text("Fetching… ");
  $.ajax({
        url: "http://api.hithot.cc/1/keywords?locale=zh_TW&cl=s&rt=a&f=jsonp",
        dataType: "jsonp",
        success: function(data) {
            var index = 0;
            var m = data.results.map(function(d) {
                var size = 100 - (index++);
                if ( typeof(d.change) != 'undefined') {
                    size = Math.min(100, Math.max(20, parseInt(d.change, 10)));
                } 

                size *= 0.9;

                return {text: d.name, size: size};
                });
            tags = m;
            generate();
          }
        });
 
}

function loadByKeywords(keywords) {
	var maxSize = 0;
    var m = keywords.map(function(d) {
        var size = d.count;
        
        if (maxSize == 0 || size > maxSize) {
        	maxSize = size;
        }
        
        if (size >= 90) {
        	size = 89 + Math.round((size / maxSize) * 10);
        }
        else {
        	size = Math.round( size / 10 ) * 10;
        }
        
        size = Math.max( 24, Math.round( size / 2 ) );

        return {text: d.name, size: size};
        });
    tags = m;
    
    console.log(tags);
    generate();
}

function generate() {
  layout
      .font("Impact")
      .spiral("archimedean");
  complete = 0;
  statusText.style("display", null);
  words = [];
  layout.stop().words(tags).start();
}

function progress(d) {
  statusText.text(++complete + "/" + max);
}

function draw(data, bounds) {
  statusText.style("display", "none");
  scale = bounds ? Math.min(
      w / Math.abs(bounds[1].x - w / 2),
      w / Math.abs(bounds[0].x - w / 2),
      h / Math.abs(bounds[1].y - h / 2),
      h / Math.abs(bounds[0].y - h / 2)) / 2 : 1;
  words = data;
  var text = vis.selectAll("text")
      .data(words, function(d) { return d.text; });
  text.transition()
      .duration(1000)
      .attr("transform", function(d) { return "translate(" + [d.x, d.y] + ")rotate(" + d.rotate + ")"; })
      .style("font-size", function(d) { return d.size + "px"; });
  text.enter().append("text")
      .attr("text-anchor", "middle")
      .attr("transform", function(d) { return "translate(" + [d.x, d.y] + ")rotate(" + d.rotate + ")"; })
      .style("font-size", function(d) { return d.size + "px"; })
      .on("click", function(d) {
        search.doSearch(d.text);
      })
      .style("opacity", 1e-6)
    .transition()
      .duration(1000)
      .style("opacity", 1);
  text.style("font-family", function(d) { return d.font; })
      .style("fill", function(d) { return fill(d.text); })
      .text(function(d) { return d.text; });
  var exitGroup = background.append("g").attr("transform", vis.attr("transform"));
  var exitGroupNode = exitGroup.node();
  text.exit().each(function() {
    exitGroupNode.appendChild(this);
  });
  exitGroup.transition()
      .duration(1000)
      .style("opacity", 1e-6)
      .remove();
  vis.transition()
      .delay(1000)
      .duration(750)
      .attr("transform", "translate(" + [w >> 1, h >> 1] + ")scale(" + scale + ")");
}

// Converts a given word cloud to image/png.
function downloadPNG() {
  var canvas = document.createElement("canvas"),
      c = canvas.getContext("2d");
  canvas.width = w;
  canvas.height = h;
  c.translate(w >> 1, h >> 1);
  c.scale(scale, scale);
  words.forEach(function(word, i) {
    c.save();
    c.translate(word.x, word.y);
    c.rotate(word.rotate * Math.PI / 180);
    c.textAlign = "center";
    c.fillStyle = fill(word.text);
    c.font = word.size + "px " + word.font;
    c.fillText(word.text, 0, 0);
    c.restore();
  });
  d3.select(this).attr("href", canvas.toDataURL("image/png"));
}

function downloadSVG() {
  d3.select(this).attr("href", "data:image/svg+xml;charset=utf-8;base64," + btoa(unescape(encodeURIComponent(
    svg.attr("version", "1.1")
       .attr("xmlns", "http://www.w3.org/2000/svg")
     .node().parentNode.innerHTML))));
}

d3.select("#random-palette").on("click", function() {
  paletteJSON("http://www.colourlovers.com/api/palettes/random", {}, function(d) {
    fill.range(d[0].colors);
    vis.selectAll("text")
        .style("fill", function(d) { return fill(d.text.toLowerCase()); });
  });
  d3.event.preventDefault();
});

(function() {
  var r = 40.5,
      px = 35,
      py = 20;

  var angles = d3.select("#angles").append("svg")
      .attr("width", 2 * (r + px))
      .attr("height", r + 1.5 * py)
    .append("g")
      .attr("transform", "translate(" + [r + px, r + py] +")");

  angles.append("path")
      .style("fill", "none")
      .attr("d", ["M", -r, 0, "A", r, r, 0, 0, 1, r, 0].join(" "));

  angles.append("line")
      .attr("x1", -r - 7)
      .attr("x2", r + 7);

  angles.append("line")
      .attr("y2", -r - 7);

  angles.selectAll("text")
      .data([-90, 0, 90])
    .enter().append("text")
      .attr("dy", function(d, i) { return i === 1 ? null : ".3em"; })
      .attr("text-anchor", function(d, i) { return ["end", "middle", "start"][i]; })
      .attr("transform", function(d) {
        d += 90;
        return "rotate(" + d + ")translate(" + -(r + 10) + ")rotate(" + -d + ")translate(2)";
      })
      .text(function(d) { return d + "°"; });

  var radians = Math.PI / 180,
      from,
      to,
      count,
      scale = d3.scale.linear(),
      arc = d3.svg.arc()
        .innerRadius(0)
        .outerRadius(r);

  d3.selectAll("#angle-count, #angle-from, #angle-to")
      .on("change", getAngles)
      .on("mouseup", getAngles);

  getAngles();

  function getAngles() {
    count = +1;
    from = Math.max(-90, Math.min(90, -30));
    to = Math.max(-90, Math.min(90, -30));
    update();
  }

  function update() {
    scale.domain([0, count - 1]).range([from, to]);
    var step = (to - from) / count;

    var path = angles.selectAll("path.angle")
        .data([{startAngle: from * radians, endAngle: to * radians}]);
    path.enter().insert("path", "circle")
        .attr("class", "angle")
        .style("fill", "#fc0");
    path.attr("d", arc);

    var line = angles.selectAll("line.angle")
        .data(d3.range(count).map(scale));
    line.enter().append("line")
        .attr("class", "angle");
    line.exit().remove();
    line.attr("transform", function(d) { return "rotate(" + (90 + d) + ")"; })
        .attr("x2", function(d, i) { return !i || i === count - 1 ? -r - 5 : -r; });

    var drag = angles.selectAll("path.drag")
        .data([from, to]);
    drag.enter().append("path")
        .attr("class", "drag")
        .attr("d", "M-9.5,0L-3,3.5L-3,-3.5Z")
        .call(d3.behavior.drag()
          .on("drag", function(d, i) {
            d = (i ? to : from) + 90;
            var start = [-r * Math.cos(d * radians), -r * Math.sin(d * radians)],
                m = [d3.event.x, d3.event.y],
                delta = ~~(Math.atan2(cross(start, m), dot(start, m)) / radians);
            d = Math.max(-90, Math.min(90, d + delta - 90)); // remove this for 360°
            delta = to - from;
            if (i) {
              to = d;
              if (delta > 360) from += delta - 360;
              else if (delta < 0) from = to;
            } else {
              from = d;
              if (delta > 360) to += 360 - delta;
              else if (delta < 0) to = from;
            }
            update();
          })
          .on("dragend", generate));
    drag.attr("transform", function(d) { return "rotate(" + (d + 90) + ")translate(-" + r + ")"; });
    layout.rotate(function() {
      return scale(~~(Math.random() * count));
    });
  }

  function cross(a, b) { return a[0] * b[1] - a[1] * b[0]; }
  function dot(a, b) { return a[0] * b[0] + a[1] * b[1]; }
})();
