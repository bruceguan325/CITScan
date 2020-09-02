var commonNestedModelParser = function(model, data) {
	//console.log("*************************************************************");
	//console.log("commonNestedModelParser start ::: ");
	for (var key in model) {
		//console.log("  -> using key ::: " + key);
		var embeddedClass = model[key];
		var embeddedData = data[key];
		//console.log("  -> embeddedClass ::: " + embeddedClass);
		//console.log(embeddedClass);
		//console.log("  -> model ::: ");
		//console.log(model);
		//console.log("  -> data ::: ");
		//console.log(data);
		
		if (typeof(embeddedClass) == 'object' && embeddedClass.constructor === [].constructor
			&& typeof(embeddedData) == 'object' && embeddedData.constructor === [].constructor
			) {
			//finalClass = embeddedClass[0]; // 假設 model class 這邊只會放一個 element
			//console.log("embeddedData.length=" + embeddedData.length);
			//console.log(embeddedData);
			
			for (var i=0; i < embeddedData.length; i++) {
				//console.log("i=" + i);
				finalClass = embeddedClass.length > i ? embeddedClass[i] : embeddedClass[0]; // 假設 model class 這邊只會放一個 element
				var tmp = new finalClass(embeddedData[i], {parse: true});
				//console.log("tmp :::");
				//console.log(tmp);
				embeddedData[i] = tmp;
			}
			
			data[key] = embeddedData;
			//console.log("  -> data after parsed ::: ");
			//console.log(data);
		}
		else if (typeof(embeddedData) != 'undefined' ) {
			data[key] = new embeddedClass(embeddedData, {parse: true});
		}
	}
	//console.log("commonNestedModelParser end ::: " + model.constructor.name);
	
	return data;
};

var NestedModel = Backbone.Model.extend({
	recursiveToJSON: function(){
		var clone = _.clone(this.attributes);
		_.each(clone, function (attr, idx) {
			if (attr != null && attr.recursiveToJSON && typeof(attr.recursiveToJSON)=='function'){
				clone[idx] = attr.recursiveToJSON();
			}
			if (attr != null && attr.constructor === [].constructor) {
				var cloneArr = _.clone(attr);
				for (var i=0; i < cloneArr.length; i++) {
					if (cloneArr[i] != null && cloneArr[i].recursiveToJSON && typeof(cloneArr[i].recursiveToJSON)=='function') {
						cloneArr[i] = cloneArr[i].recursiveToJSON();
					}
				}
				clone[idx] = cloneArr;
			}
		});
		return clone;
	}
});

var ImagemapActionModel = NestedModel.extend({
	name: "ImagemapActionModel",
	defaults: { type: "message" },
	model: {},
	validate: function(attrs, options) {
		if (attrs.bounds) {
			if (!attrs.label) {
				return 'Action title is required';
			}
			if ((attrs.type == 'message' && (!attrs.text || attrs.text == ''))
				|| (attrs.type == 'uri' && (!attrs.uri || attrs.uri == ''))
				|| (attrs.type == 'postback' && (!attrs.data || attrs.data == ''))
				) {
				return 'Action data is required';
			}
			if (attrs.type == 'uri' && !(attrs.uri.startsWith("http") || attrs.uri.startsWith("line") || attrs.uri.startsWith("tel"))) {
				return 'The available schemes are http, https, line, and tel.';
			}
		}
	},
	parse: function(data) {
		if (data.action) {
			jQuery.each(data.action, function(i, val) {
				//console.log(i+'===>'+val)
				data[i] = val;
			});
			delete data.action;
		}
		data = commonNestedModelParser(this.model, data);
		return data;
	}});

var ImagemapMessageModel = NestedModel.extend({
	name: "ImagemapMessageModel",
	defaults: { type: "richmenu", areas: []},
	model: { areas:[ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel] },  // 30 個
	validate: function(attrs, options) {
		if (!attrs.imageMapType || attrs.imageMapType == '' || attrs.imageMapType == 'type99') {
			return 'ImageMapType is required';
		}
		if (!attrs.baseUrl || attrs.baseUrl == '') {
			return 'BaseUrl is required';
		}
		if (!attrs.areas) {
			return 'Areas is required';
		}
		else {
			for (var idx=0; idx < attrs.areas.length; idx++) {
				action = attrs.areas[idx];
				r = action.validate(action.attributes, options);
				if (typeof(r) != 'undefined') {
					return r;
				}
			}
		}
	},
	initialize: function() {
        //console.log('ImagemapMessageModel: initialize');
		this.on('change:imageMapType', this.onImageMapTypeChange);
		
		// 有預設值就 trigger 一下
		var imt = this.get("imageMapType");
		if (imt != null) {
			this.trigger('change:imageMapType', this, imt);
		}
	},
	parse: function(data) {
        //console.log('ImagemapMessageModel: parse');
		data = commonNestedModelParser(this.model, data);
		return data;
	},
	onImageMapTypeChange: function(model, value) {
        //console.log('ImagemapMessageModel: imageMapType was changed to:', value);
        var areas = this.get("areas");
        var bounds = this.theDefaultAreaSettings[value];
        //如果一開始已有高度資訊，則以現有的為主
        if (defaultImgHeight == 843 || (this.get("size") && this.get("size").height == 843)) {
        	//console.log('843');
        	bounds = this.theHalfAreaSettings[value];
            this.set("size", {width: 2500, height: 843});
        } else {
        	//console.log('1686');
        	this.set("size", {width: 2500, height: 1686});
        }
        //console.log('----->'+defaultImgHeight);
        //console.log(bounds.length);
        //console.log(bounds);
        //console.log(areas.length);
        //console.log(areas);
        //console.log('----->'+value);
        //決定imageMapType範圍
        for (var i=0; i < bounds.length && i < areas.length; i++) {
       		areas[i].set("bounds", bounds[i]);
        }
        //超過的部分則初始化
        for (var i=bounds.length; i >= bounds.length && i < areas.length; i++) {
        	areas[i].set("type", "message");
        	areas[i].unset("bounds");
        	areas[i].unset("label");
        	areas[i].unset("text");
        	areas[i].unset("uri");
        }
        
        if (areas.length > bounds.length && value != "type99") {
            for (var i=areas.length; i < areas.length; i++) {
           		areas[i].set("bounds", {x:0, y:0, width:0, height: 0});
            }
        }
	},
	theDefaultAreaSettings: { // 這些都是預設好不同 type 對應的按鈕 area
    		type1: [ 
    			{ "x": 0, "y": 0, "width": 2500, "height": 1686 }, 
    		],
    		type2: [ 
    			{ "x": 0, "y": 0, "width": 1250, "height": 1686 }, 
    			{ "x": 1250, "y": 0, "width": 1250, "height": 1686 }, 
    		],
    		type3: [ 
    			{ "x": 0, "y": 0, "width": 2500, "height": 843 }, 
    			{ "x": 0, "y": 843, "width": 2500, "height": 843 }, 
    		],
    		type4: [ 
    			{ "x": 0, "y": 0, "width": 2500, "height": 562 }, 
    			{ "x": 0, "y": 562, "width": 2500, "height": 562 }, 
    			{ "x": 0, "y": 1124, "width": 2500, "height": 562 }, 
    		],
    		type5: [ 
    			{ "x": 0, "y": 0, "width": 1250, "height": 843 }, 
    			{ "x": 1250, "y": 0, "width": 1250, "height": 843 }, 
    			{ "x": 0, "y": 843, "width": 1250, "height": 843 }, 
    			{ "x": 1250, "y": 843, "width": 1250, "height": 843 }, 
    		],
    		type6: [ 
    			{ "x": 0, "y": 0, "width": 2500, "height": 843 }, 
    			{ "x": 0, "y": 843, "width": 1250, "height": 843 }, 
    			{ "x": 1250, "y": 843, "width": 1250, "height": 843 }, 
    		],
    		type7: [ 
    			{ "x": 0, "y": 0, "width": 2500, "height": 843 }, 
    			{ "x": 0, "y": 843, "width": 2500, "height": 421 }, 
    			{ "x": 0, "y": 1264, "width": 2500, "height": 422 }, 
    		],
    		type8: [ 
    			{ "x": 0, "y": 0, "width": 1250, "height": 843 }, 
    			{ "x": 1250, "y": 0, "width": 1250, "height": 843 }, 
    			{ "x": 0, "y": 843, "width": 833, "height": 843 }, 
    			{ "x": 833, "y": 843, "width": 833, "height": 843 }, 
    			{ "x": 1666, "y": 843, "width": 834, "height": 843 }, 
    		],
    		type9: [ 
    			{ "x": 0, "y": 0, "width": 833, "height": 843 }, 
    			{ "x": 833, "y": 0, "width": 833, "height": 843 }, 
    			{ "x": 1666, "y": 0, "width": 834, "height": 843 }, 
    			{ "x": 0, "y": 843, "width": 1250, "height": 843 }, 
    			{ "x": 1250, "y": 843, "width": 1250, "height": 843 }, 
    		],
    		type10: [ 
    			{ "x": 0, "y": 0, "width": 833, "height": 843 }, 
    			{ "x": 833, "y": 0, "width": 833, "height": 843 }, 
    			{ "x": 1666, "y": 0, "width": 834, "height": 843 }, 
    			{ "x": 0, "y": 843, "width": 833, "height": 843 }, 
    			{ "x": 833, "y": 843, "width": 833, "height": 843 }, 
    			{ "x": 1666, "y": 843, "width": 834, "height": 843 }, 
    		],
    		type11: [ 
    			{ "x": 0, "y": 0, "width": 833, "height": 562 }, 
    			{ "x": 833, "y": 0, "width": 833, "height": 562 }, 
    			{ "x": 1666, "y": 0, "width": 834, "height": 562 }, 
    			{ "x": 0, "y": 562, "width": 833, "height": 562 }, 
    			{ "x": 833, "y": 562, "width": 833, "height": 562 }, 
    			{ "x": 1666, "y": 562, "width": 834, "height": 562 }, 
    			{ "x": 0, "y": 1124, "width": 833, "height": 562 }, 
    			{ "x": 833, "y": 1124, "width": 833, "height": 562 }, 
    			{ "x": 1666, "y": 1124, "width": 834, "height": 562 }, 
    		],
    		type12: [
    			{ "x": 0, "y": 0, "width": 1250, "height": 562 }, 
    			{ "x": 1250, "y": 0, "width": 1250, "height": 562 }, 
    			{ "x": 0, "y": 562, "width": 1250, "height": 562 }, 
    			{ "x": 1250, "y": 562, "width": 1250, "height": 562 }, 
    			{ "x": 0, "y": 1124, "width": 1250, "height": 562 }, 
    			{ "x": 1250, "y": 1124, "width": 1250, "height": 562 }, 
    		],
    		type13: [
    			{ "x": 0, "y": 0, "width": 1250, "height": 421 },
    			{ "x": 1250, "y": 0, "width": 1250, "height": 421 },
    			{ "x": 0, "y": 421, "width": 1250, "height": 422 },
    			{ "x": 1250, "y": 421, "width": 1250, "height": 422 },
    			{ "x": 0, "y": 843, "width": 1250, "height": 421 },
    			{ "x": 1250, "y": 843, "width": 1250, "height": 421 },
    			{ "x": 0, "y": 1264, "width": 1250, "height": 422 },
    			{ "x": 1250, "y": 1264, "width": 1250, "height": 422 },
    		],
    		type14: [
    			{ "x": 0, "y": 0, "width": 833, "height": 337 },
    			{ "x": 833, "y": 0, "width": 833, "height": 337 },
    			{ "x": 1666, "y": 0, "width": 834, "height": 337 },
    			{ "x": 0, "y": 337, "width": 833, "height": 337 },
    			{ "x": 833, "y": 337, "width": 833, "height": 337 },
    			{ "x": 1666, "y": 337, "width": 834, "height": 337 },
    			{ "x": 0, "y": 674, "width": 833, "height": 337 },
    			{ "x": 833, "y": 674, "width": 833, "height": 337 },
    			{ "x": 1666, "y": 674, "width": 834, "height": 337 },
    			{ "x": 0, "y": 1011, "width": 833, "height": 337 },
    			{ "x": 833, "y": 1011, "width": 833, "height": 337 },
    			{ "x": 1666, "y": 1011, "width": 834, "height": 337 },
    			{ "x": 0, "y": 1348, "width": 833, "height": 338 },
    			{ "x": 833, "y": 1348, "width": 833, "height": 338 },
    			{ "x": 1666, "y": 1348, "width": 834, "height": 338 },
    		],
    		type99: [
    			// type99 代表 free style, 你有 free style 嗎？
    		],
    },
	theHalfAreaSettings: { // 這些都是預設好不同 type 對應的按鈕 area
		type1: [ 
			{ "x": 0, "y": 0, "width": 2500, "height": 843 }, 
		],
		type2: [ 
			{ "x": 0, "y": 0, "width": 1250, "height": 843 }, 
			{ "x": 1250, "y": 0, "width": 1250, "height": 843 }, 
		],
		type3: [ 
			{ "x": 0, "y": 0, "width": 2500, "height": 421 }, 
			{ "x": 0, "y": 421, "width": 2500, "height": 422 }, 
		],
		type4: [ 
			{ "x": 0, "y": 0, "width": 2500, "height": 281 }, 
			{ "x": 0, "y": 281, "width": 2500, "height": 281 }, 
			{ "x": 0, "y": 562, "width": 2500, "height": 281 }, 
		],
		type5: [ 
			{ "x": 0, "y": 0, "width": 1250, "height": 421 }, 
			{ "x": 1250, "y": 0, "width": 1250, "height": 421 }, 
			{ "x": 0, "y": 421, "width": 1250, "height": 422 }, 
			{ "x": 1250, "y": 421, "width": 1250, "height": 422 }, 
		],
		type6: [ 
			{ "x": 0, "y": 0, "width": 2500, "height": 421 }, 
			{ "x": 0, "y": 421, "width": 1250, "height": 422 }, 
			{ "x": 1250, "y": 421, "width": 1250, "height": 422 }, 
		],
		type7: [ 
			{ "x": 0, "y": 0, "width": 2500, "height": 421 }, 
			{ "x": 0, "y": 421, "width": 2500, "height": 211 }, 
			{ "x": 0, "y": 632, "width": 2500, "height": 211 }, 
		],
		type8: [ 
			{ "x": 0, "y": 0, "width": 1250, "height": 421 }, 
			{ "x": 1250, "y": 0, "width": 1250, "height": 421 }, 
			{ "x": 0, "y": 421, "width": 833, "height": 422 }, 
			{ "x": 833, "y": 421, "width": 833, "height": 422 }, 
			{ "x": 1666, "y": 421, "width": 834, "height": 422 }, 
		],
		type9: [ 
			{ "x": 0, "y": 0, "width": 833, "height": 421 }, 
			{ "x": 833, "y": 0, "width": 833, "height": 421 }, 
			{ "x": 1666, "y": 0, "width": 834, "height": 421 }, 
			{ "x": 0, "y": 421, "width": 1250, "height": 422 }, 
			{ "x": 1250, "y": 421, "width": 1250, "height": 422 }, 
		],
		type10: [ 
			{ "x": 0, "y": 0, "width": 833, "height": 421 }, 
			{ "x": 833, "y": 0, "width": 833, "height": 421 }, 
			{ "x": 1666, "y": 0, "width": 834, "height": 421 }, 
			{ "x": 0, "y": 421, "width": 833, "height": 422 }, 
			{ "x": 833, "y": 421, "width": 833, "height": 422 }, 
			{ "x": 1666, "y": 421, "width": 834, "height": 422 }, 
		],
		type11: [ 
			{ "x": 0, "y": 0, "width": 833, "height": 281 }, 
			{ "x": 833, "y": 0, "width": 833, "height": 281 }, 
			{ "x": 1666, "y": 0, "width": 834, "height": 281 }, 
			{ "x": 0, "y": 281, "width": 833, "height": 281 }, 
			{ "x": 833, "y": 281, "width": 833, "height": 281 }, 
			{ "x": 1666, "y": 281, "width": 834, "height": 281 }, 
			{ "x": 0, "y": 562, "width": 833, "height": 281 }, 
			{ "x": 833, "y": 562, "width": 833, "height": 281 }, 
			{ "x": 1666, "y": 562, "width": 834, "height": 281 }, 
		],
		type12: [
			{ "x": 0, "y": 0, "width": 1250, "height": 281 }, 
			{ "x": 1250, "y": 0, "width": 1250, "height": 281 }, 
			{ "x": 0, "y": 281, "width": 1250, "height": 281 }, 
			{ "x": 1250, "y": 281, "width": 1250, "height": 281 }, 
			{ "x": 0, "y": 562, "width": 1250, "height": 281 }, 
			{ "x": 1250, "y": 562, "width": 1250, "height": 281 }, 
		],
		type13: [
			{ "x": 0, "y": 0, "width": 1250, "height": 210 },
			{ "x": 1250, "y": 0, "width": 1250, "height": 210 },
			{ "x": 0, "y": 210, "width": 1250, "height": 211 },
			{ "x": 1250, "y": 210, "width": 1250, "height": 211 },
			{ "x": 0, "y": 421, "width": 1250, "height": 211 },
			{ "x": 1250, "y": 421, "width": 1250, "height": 211 },
			{ "x": 0, "y": 632, "width": 1250, "height": 211 },
			{ "x": 1250, "y": 632, "width": 1250, "height": 211 },
		],
		type14: [
			{ "x": 0, "y": 0, "width": 833, "height": 168 },
			{ "x": 833, "y": 0, "width": 833, "height": 168 },
			{ "x": 1666, "y": 0, "width": 834, "height": 168 },
			{ "x": 0, "y": 168, "width": 833, "height": 168 },
			{ "x": 833, "y": 168, "width": 833, "height": 168 },
			{ "x": 1666, "y": 168, "width": 834, "height": 168 },
			{ "x": 0, "y": 336, "width": 833, "height": 169 },
			{ "x": 833, "y": 336, "width": 833, "height": 169 },
			{ "x": 1666, "y": 336, "width": 834, "height": 169 },
			{ "x": 0, "y": 505, "width": 833, "height": 169 },
			{ "x": 833, "y": 505, "width": 833, "height": 169 },
			{ "x": 1666, "y": 505, "width": 834, "height": 169 },
			{ "x": 0, "y": 674, "width": 833, "height": 169 },
			{ "x": 833, "y": 674, "width": 833, "height": 169 },
			{ "x": 1666, "y": 674, "width": 834, "height": 169 },
		],
		type99: [
			// type99 代表 free style, 你有 free style 嗎？
		],
}
	});

var RichMessageModel = NestedModel.extend({
	name: "RichMessageModel",
	urlRoot: '/wise/wiseadm/webRM/richMenu',  // context path 是個坑？
	defaults: {
		id: null,
		mkey: null,
		tenantId: null,
		updateTime: null,
		createdTime: null,
		msgName: null,
		msgDesc: null,
		msgTemplate: new ImagemapMessageModel({ type: "richmenu" }, {parse: true}),
		msgType: "richmenu"
	},
	validate: function(attrs, options) {
		console.log('Doing validation');
		if (!attrs.mkey || attrs.mkey == '') {
			return 'Mkey is required';
		}
		if (!attrs.msgName || attrs.msgName == '') {
			return 'Message name is required';
		}
		if (!attrs.msgDesc || attrs.msgDesc == '') {
			return 'Message description is required';
		}
		if (!attrs.menuSeq || attrs.menuSeq == '') {
			return 'Message sequence is required';
		}
		if (!attrs.channelCode || attrs.channelCode == '') {
			return 'Channel is required';
		}
		if (attrs.msgTemplate) {
			r = attrs.msgTemplate.validate(attrs.msgTemplate.attributes, options);
			
			if (typeof(r) != 'undefined') {
				return r;
			}
		}
	},
	parse: function(data) {
		if (data.msgType == 'richmenu') {
			data.msgTemplate = new ImagemapMessageModel(data.msgTemplate, {parse: true});
		}
		return data;
	}
});

var RichMessageCollection = Backbone.Collection.extend({
	url: '/wise/wiseadm/webRM/richMenu',
	model: RichMessageModel,
	parse: function(colData) {
		_.forEach(colData, function(data) {
			data.msgTemplate = JSON.parse(data.msgTemplate);
		});
		return colData;
	},
});
