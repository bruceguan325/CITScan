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

var MessageActionModel = NestedModel.extend({
	name: "MessageActionModel",
	defaults: { type: "message" },
	model: {},
	validate: function(attrs, options) {
		if (attrs.label) { // 有 label 才做檢核，沒 label 的 action 在儲存時送到 servlet 後會被刪掉
			if ((attrs.type == 'message' && (!attrs.text || attrs.text == ''))
				|| (attrs.type == 'uri' && (!attrs.uri || attrs.uri == ''))
				|| (attrs.type == 'postback' && (!attrs.data || attrs.data == ''))
				) {
				return 'Action data is required';
			}
		}
	},
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		return data;
	}});

var ImagemapActionModel = NestedModel.extend({
	name: "ImagemapActionModel",
	defaults: { type: "message" },
	model: {},
	validate: function(attrs, options) {
		if (attrs.label) { // 有 label 才做檢核，沒 label 的 action 在儲存時送到 servlet 後會被刪掉
			if ((attrs.type == 'message' && (!attrs.text || attrs.text == ''))
				|| (attrs.type == 'uri' && (!attrs.linkUri || attrs.linkUri == ''))
				|| (attrs.type == 'postback' && (!attrs.data || attrs.data == ''))
				) {
				return 'Action data is required';
			}
		}
	},
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		return data;
	}});

var ButtonsMessageModel = NestedModel.extend({
	name: "ButtonsMessageModel",
	model: { actions:[MessageActionModel, MessageActionModel, MessageActionModel, MessageActionModel] },
	validate: function(attrs, options) {
		if (!attrs.type || attrs.type == '') {
			return 'Type is required';
		}
		if (!attrs.text || attrs.text == '') {
			return 'Text is required';
		}
		if (!attrs.actions) {
			return 'Actions is required';
		}
		else {
			for (var idx=0; idx < attrs.actions.length; idx++) {
				action = attrs.actions[idx];
				r = action.validate(action.attributes, options);
				if (typeof(r) != 'undefined') {
					return r;
				}
			}
		}
	},
	parse: function(data) {
		//console.log(this.model);
		//console.log(data);
		data = commonNestedModelParser(this.model, data);
		return data;
	}});
var CarouselMessageColumnModel = NestedModel.extend({
	name: "CarouselMessageColumnModel",
	model: { actions:[MessageActionModel, MessageActionModel, MessageActionModel] },
	validate: function(attrs, options) {
		if (attrs.text && attrs.text != '') { // Column.text 是必要，若沒有填視為本 column 是要刪除的
			if (!attrs.actions) {
				return 'Actions is required';
			}
			else {
				for (var idx=0; idx < attrs.actions.length; idx++) {
					action = attrs.actions[idx];
					r = action.validate(action.attributes, options);
					if (typeof(r) != 'undefined') {
						return r;
					}
				}
			}
		}
	},
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		//console.log(data);
		return data;
	}});
var CarouselMessageModel = NestedModel.extend({
	name: "CarouselMessageModel",
	defaults: { type: "carousel", columns: [] },
	model: { columns:[CarouselMessageColumnModel, CarouselMessageColumnModel, CarouselMessageColumnModel, CarouselMessageColumnModel, CarouselMessageColumnModel, CarouselMessageColumnModel, CarouselMessageColumnModel, CarouselMessageColumnModel, CarouselMessageColumnModel, CarouselMessageColumnModel] },
	validate: function(attrs, options) {
		if (!attrs.columns) {
			return 'Columns is required';
		}
		else {
			for (var idx=0; idx < attrs.columns.length; idx++) {
				col = attrs.columns[idx];
				r = col.validate(col.attributes, options);
				if (typeof(r) != 'undefined') {
					return r;
				}
			}
		}
	},
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		return data;
	}});
var TemplateMessageModel = NestedModel.extend({
	name: "TemplateMessageModel",
	model: {},
	validate: function(attrs, options) {
		//console.log("template message validation");
		if (!attrs.reward || attrs.reward == '') {
			return 'Reward is required';
		}
		if (attrs.template) {
			r = attrs.template.validate(attrs.template.attributes, options);
			
			if (typeof(r) != 'undefined') {
				return r;
			}
		}
	},
	parse: function(data) {
		if (data != null && data.template != null) {
		//if (!data.template instanceof Backbone.Model) {
			if (data.template.type == 'buttons') {
				data.template = new ButtonsMessageModel(data.template, {parse: true});
			}
			else if (data.template.type == 'carousel') {
				data.template = new CarouselMessageModel(data.template, {parse: true});
			}
		//}
		}
		return data;
	}});
var ImagemapMessageModel = NestedModel.extend({
	name: "ImagemapMessageModel",
	defaults: { type: "imagemap", imageMapType: "type1", baseSize: { width: 1040, height: 1040 }, actions: []},
	model: { actions:[ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel] },  // 30 個
	validate: function(attrs, options) {
		if (!attrs.reward || attrs.reward == '') {
			return 'Reward is required';
		}
		if (!attrs.actions) {
			return 'Actions is required';
		}
		else {
			for (var idx=0; idx < attrs.actions.length; idx++) {
				action = attrs.actions[idx];
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
        
        var actions = this.get("actions");
        var areas = this.theDefaultAreaSettings[value];
        this.set("baseSize", {width: 1040, height: 1040});
        
        for (var i=0; i < areas.length && i < actions.length; i++) {
       		actions[i].set("area", areas[i]);
        }
        
        if (actions.length > areas.length && value != "type99") {
            for (var i=areas.length; i < actions.length; i++) {
           		actions[i].set("area", {x:0, y:0, width:0, height: 0});
            }
        }
        
//        console.log(this.get("actions"));
	},
	theDefaultAreaSettings: { // 這些都是預設好不同 type 對應的按鈕 area
    		type1: [ 
    			{ "x": 0, "y": 0, "width": 1040, "height": 1040 }, 
    		],
    		type2: [ 
    			{ "x": 0, "y": 0, "width": 520, "height": 1040 }, 
    			{ "x": 520, "y": 0, "width": 520, "height": 1040 }, 
    		],
    		type3: [ 
    			{ "x": 0, "y": 0, "width": 1040, "height": 520 }, 
    			{ "x": 0, "y": 520, "width": 1040, "height": 520 }, 
    		],
    		type4: [ 
    			{ "x": 0, "y": 0, "width": 1040, "height": 347 }, 
    			{ "x": 0, "y": 347, "width": 1040, "height": 347 }, 
    			{ "x": 0, "y": 694, "width": 1040, "height": 346 }, 
    		],
    		type5: [ 
    			{ "x": 0, "y": 0, "width": 520, "height": 520 }, 
    			{ "x": 520, "y": 0, "width": 520, "height": 520 }, 
    			{ "x": 0, "y": 520, "width": 520, "height": 520 }, 
    			{ "x": 520, "y": 520, "width": 520, "height": 520 }, 
    		],
    		type6: [ 
    			{ "x": 0, "y": 0, "width": 1040, "height": 520 }, 
    			{ "x": 0, "y": 520, "width": 520, "height": 520 }, 
    			{ "x": 520, "y": 520, "width": 520, "height": 520 }, 
    		],
    		type7: [ 
    			{ "x": 0, "y": 0, "width": 1040, "height": 520 }, 
    			{ "x": 0, "y": 520, "width": 1040, "height": 260 }, 
    			{ "x": 0, "y": 780, "width": 1040, "height": 260 }, 
    		],
    		type8: [ 
    			{ "x": 0, "y": 0, "width": 520, "height": 520 }, 
    			{ "x": 520, "y": 0, "width": 520, "height": 520 }, 
    			{ "x": 0, "y": 520, "width": 347, "height": 520 }, 
    			{ "x": 347, "y": 520, "width": 347, "height": 520 }, 
    			{ "x": 694, "y": 520, "width": 346, "height": 520 }, 
    		],
    		type9: [ 
    			{ "x": 0, "y": 0, "width": 347, "height": 520 }, 
    			{ "x": 347, "y": 0, "width": 347, "height": 520 }, 
    			{ "x": 694, "y": 0, "width": 346, "height": 520 }, 
    			{ "x": 0, "y": 520, "width": 520, "height": 520 }, 
    			{ "x": 520, "y": 520, "width": 520, "height": 520 }, 
    		],
    		type10: [ 
    			{ "x": 0, "y": 0, "width": 347, "height": 520 }, 
    			{ "x": 347, "y": 0, "width": 347, "height": 520 }, 
    			{ "x": 694, "y": 0, "width": 346, "height": 520 }, 
    			{ "x": 0, "y": 520, "width": 347, "height": 520 }, 
    			{ "x": 347, "y": 520, "width": 347, "height": 520 }, 
    			{ "x": 694, "y": 520, "width": 346, "height": 520 }, 
    		],
    		type11: [ 
    			{ "x": 0, "y": 0, "width": 347, "height": 347 }, 
    			{ "x": 347, "y": 0, "width": 347, "height": 347 }, 
    			{ "x": 694, "y": 0, "width": 346, "height": 347 }, 
    			{ "x": 0, "y": 347, "width": 347, "height": 347 }, 
    			{ "x": 347, "y": 347, "width": 347, "height": 347 }, 
    			{ "x": 694, "y": 347, "width": 346, "height": 347 }, 
    			{ "x": 0, "y": 694, "width": 347, "height": 346 }, 
    			{ "x": 347, "y": 694, "width": 347, "height": 346 }, 
    			{ "x": 694, "y": 694, "width": 346, "height": 346 }, 
    		],
    		type99: [
    			// type99 代表 free style, 你有 free style 嗎？
    		],
    }
	});

var RichMessageModel = NestedModel.extend({
	name: "RichMessageModel",
	urlRoot: '/wise/wiseadm/citi/richMessages',  // context path 是個坑？
	defaults: {
		id: null,
		tenantId: null,
		updateTime: null,
		createdTime: null,
		expireTime: null,
		msgTemplate: new TemplateMessageModel({ type: "template", template: {type: "carousel", columns: [{actions: []}] } }, {parse: true}),
		msgType: "carousel",
		priority: null,
		cardType: null,
		title: null
	},
	validate: function(attrs, options) {
		console.log('Doing validation');
		if (!attrs.priority || attrs.priority == '') {
			return 'Priority is required';
		}
		if (!attrs.cardType || attrs.cardType == '') {
			return 'Logo name is required';
		}
		if (!attrs.title || attrs.title == '') {
			return 'Title description is required';
		}
		
		if (attrs.msgTemplate) {
			r = attrs.msgTemplate.validate(attrs.msgTemplate.attributes, options);
			
			if (typeof(r) != 'undefined') {
				return r;
			}
		}
	},
	parse: function(data) {
		if (data.msgType == 'buttons') {
			data.msgTemplate = new TemplateMessageModel(data.msgTemplate, {parse: true});
		}
		else if (data.msgType == 'carousel') {
			data.msgTemplate = new TemplateMessageModel(data.msgTemplate, {parse: true});
		}
		else if (data.msgType == 'imagemap') {
			data.msgTemplate = new ImagemapMessageModel(data.msgTemplate, {parse: true});
		}
		return data;
	}
});

var RichMessageCollection = Backbone.Collection.extend({
	url: '/wise/wiseadm/citi/richMessages',
	model: RichMessageModel,
	parse: function(colData) {
		_.forEach(colData, function(data) {
			data.msgTemplate = JSON.parse(data.msgTemplate);
		});
		return colData;
	},
});