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
				|| (attrs.type == 'call' && (!attrs.phone || attrs.phone == ''))
				) {
				return 'Action data is required';
			}
			if (attrs.type == 'message') {
				attrs.uri = '';
				attrs.data = '';
				attrs.phone = '';
			} else if (attrs.type == 'uri') {
				attrs.text = '';
				attrs.data = '';
				attrs.phone = '';
			} else if (attrs.type == 'postback') {
				attrs.text = '';
				attrs.uri = '';				
				attrs.phone = '';
			} else if (attrs.type == 'call') {
				attrs.text = '';
				attrs.uri = '';
				attrs.data = '';				
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

var QuickRepliesActionModel = NestedModel.extend({
	name: "QuickRepliesActionModel",
	defaults: { type: "message" },
	model: { },
	validate: function(attrs, options) {
		if (attrs.label) { // 有 label 才做檢核，沒 label 的 action 在儲存時送到 servlet 後會被刪掉
			if ((attrs.type == 'message' && (!attrs.text || attrs.text == ''))
				|| (attrs.type == 'postback' && (!attrs.data || attrs.data == ''))
				) {
				return 'Action data is required';
			}
		}
	},
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		return data;
	}
});

var ModalActionModel = NestedModel.extend({
	name: "ModalActionModel",
	defaults: { type: "message" },
	model: {},
	validate: function(attrs, options) {
		if (attrs.label) { // 有 label 才做檢核，沒 label 的 action 在儲存時送到 servlet 後會被刪掉
			if ((attrs.type == 'message' && (!attrs.text || attrs.text == ''))
				|| (attrs.type == 'uri' && (!attrs.uri || attrs.uri == ''))
				|| (attrs.type == 'postback' && (!attrs.data || attrs.data == ''))
				|| (attrs.type == 'call' && (!attrs.phone || attrs.phone == ''))
				) {
				return 'Action data is required';
			}
			if (attrs.type == 'message') {
				attrs.uri = '';
				attrs.data = '';
				attrs.phone = '';
			} else if (attrs.type == 'uri') {
				attrs.text = '';
				attrs.data = '';
				attrs.phone = '';
			} else if (attrs.type == 'postback') {
				attrs.text = '';
				attrs.uri = '';				
				attrs.phone = '';
			} else if (attrs.type == 'call') {
				attrs.text = '';
				attrs.uri = '';
				attrs.data = '';				
			}
		} else {
			return 'noLabel';
		}
	},
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		return data;
	}});

var AlertActionModel = NestedModel.extend({
	name: "AlertActionModel",
	defaults: { type: "cancel" },
	model: {},
	validate: function(attrs, options) {
		if (attrs.label) { // 有 label 才做檢核，沒 label 的 action 在儲存時送到 servlet 後會被刪掉
			if ((attrs.type == 'uri' && (!attrs.uri || attrs.uri == ''))
				|| (attrs.type == 'call' && (!attrs.phone || attrs.phone == ''))
				) {
				return 'Action data is required';
			}
			if (attrs.type == 'cancel') {
				attrs.uri = '';
				attrs.phone = '';
			} else if (attrs.type == 'uri') {
				attrs.phone = '';
			} else if (attrs.type == 'call') {
				attrs.uri = '';
			}
		}
	},
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		return data;
	}});

var QuickRepliesItemsModel = NestedModel.extend({
	name: "QuickRepliesItemsModel",
	model: { items:[QuickRepliesActionModel, QuickRepliesActionModel, QuickRepliesActionModel,
		QuickRepliesActionModel, QuickRepliesActionModel, QuickRepliesActionModel, 
		QuickRepliesActionModel, QuickRepliesActionModel, QuickRepliesActionModel, 
		QuickRepliesActionModel, QuickRepliesActionModel, QuickRepliesActionModel, 
		QuickRepliesActionModel, QuickRepliesActionModel] },
	validate: function(attrs, options) {
		if (!attrs.items) {
			return 'Items data is required';
		} else {
			for (var idx=0; idx < attrs.items.length; idx++) {
				item = attrs.items[idx];
				r = item.validate(item.attributes, options);
				if (typeof(r) != 'undefined') {
					return r;
				}
			}
		}
	},
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		return data;
	}
});
var TextMessageModel = NestedModel.extend({
	name: "TextMessageModel",
	validate: function(attrs, options) {
		if (!attrs.text || attrs.text == '') {
			return 'Text is required';
		}
	},
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		return data;
	}});
var StretchMessageModel = NestedModel.extend({
	name: "StretchMessageModel",
	model: { actions:[MessageActionModel, MessageActionModel, MessageActionModel, MessageActionModel
		, MessageActionModel, MessageActionModel, MessageActionModel, MessageActionModel
		, MessageActionModel, MessageActionModel] },
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
		data = commonNestedModelParser(this.model, data);
		return data;
	}});
var ButtonsMessageModel = NestedModel.extend({
	name: "ButtonsMessageModel",
	model: { actions:[MessageActionModel, MessageActionModel, MessageActionModel, MessageActionModel
		, MessageActionModel, MessageActionModel, MessageActionModel, MessageActionModel
		, MessageActionModel, MessageActionModel] },
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
		data = commonNestedModelParser(this.model, data);
		return data;
	}});
var CarouselMessageColumnModel = NestedModel.extend({
	name: "CarouselMessageColumnModel",
	model: { actions:[MessageActionModel, MessageActionModel, MessageActionModel, MessageActionModel, MessageActionModel] },
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
	defaults: { type: "carousel", columns: [], fixedTitle: "" },
	model: { columns:[CarouselMessageColumnModel, CarouselMessageColumnModel, CarouselMessageColumnModel, CarouselMessageColumnModel,
		CarouselMessageColumnModel, CarouselMessageColumnModel, CarouselMessageColumnModel, CarouselMessageColumnModel, CarouselMessageColumnModel,
		CarouselMessageColumnModel, CarouselMessageColumnModel, CarouselMessageColumnModel, CarouselMessageColumnModel, CarouselMessageColumnModel,
		CarouselMessageColumnModel] },
	validate: function(attrs, options) {
		if (!attrs.fixedTitle || attrs.fixedTitle == '') {
			return 'FixedTitle is required';
		}
		
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

// one grid 圖文維護form model
var OneGridMessageColumnModel = NestedModel.extend({
  name: "OneGridMessageColumnModel",
  model: { 
    actions: [MessageActionModel] 
  },
  validate: function(attrs, options) {
    if (attrs.title && attrs.title != '') { // Column.title 是必要，若沒有填視為本 column 是要刪除的
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
    return data;
  }
});

// one grid 圖文示意圖卡、固定內文model
var OneGridMessageModel = NestedModel.extend({
  name: "OneGridMessageModel",
  defaults: {
    type: "onegrid", 
    columns: [], 
    fixedTitle: ""
  },
  model: { 
    // 維護 15 column form
    columns:[
      OneGridMessageColumnModel, 
      OneGridMessageColumnModel, 
      OneGridMessageColumnModel, 
      OneGridMessageColumnModel,
      OneGridMessageColumnModel, 
      OneGridMessageColumnModel, 
      OneGridMessageColumnModel, 
      OneGridMessageColumnModel,
      OneGridMessageColumnModel,
      OneGridMessageColumnModel, 
      OneGridMessageColumnModel, 
      OneGridMessageColumnModel, 
      OneGridMessageColumnModel, 
      OneGridMessageColumnModel,
      OneGridMessageColumnModel] 
  },
  validate: function(attrs, options) {
    if (!attrs.fixedTitle || attrs.fixedTitle == '') {
      return 'FixedTitle is required';
    }
    
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
  }
});

var ThreeGridMessageColumnModel = NestedModel.extend({
	name: "ThreeGridMessageColumnModel",
	defaults: { actions: [], title: "", text1: "", text2: "", text3: "", text4: "", text5: "", text6: "", text7: "" },
	model: { actions: [MessageActionModel] },
	validate: function(attrs, options) {
		if (attrs.title && attrs.title != '') { // Column.title 是必要，若沒有填視為本 column 是要刪除的
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
		return data;
	}});
var ThreeGridMessageModel = NestedModel.extend({
	name: "ThreeGridMessageModel",
	defaults: { type: "threegrid", columns: [], fixedTitle: ""},
	model: { columns:[ThreeGridMessageColumnModel, ThreeGridMessageColumnModel, ThreeGridMessageColumnModel, ThreeGridMessageColumnModel,
		ThreeGridMessageColumnModel, ThreeGridMessageColumnModel, ThreeGridMessageColumnModel, ThreeGridMessageColumnModel, ThreeGridMessageColumnModel,
		ThreeGridMessageColumnModel, ThreeGridMessageColumnModel, ThreeGridMessageColumnModel, ThreeGridMessageColumnModel, ThreeGridMessageColumnModel,
		ThreeGridMessageColumnModel] },
	validate: function(attrs, options) {
		if (!attrs.fixedTitle || attrs.fixedTitle == '') {
			return 'FixedTitle is required';
		}
		
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
var TwoGridMessageColumnModel = NestedModel.extend({
	name: "TwoGridMessageColumnModel",
	defaults: { title: "", text1: "", text2: "" },
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		return data;
	}});
var TwoGridMessageModel = NestedModel.extend({
	name: "TwoGridMessageModel",
	defaults: { type: "twogrid", columns: [], fixedTitle: ""},
	model: { columns:[TwoGridMessageColumnModel, TwoGridMessageColumnModel, TwoGridMessageColumnModel, TwoGridMessageColumnModel,
		TwoGridMessageColumnModel, TwoGridMessageColumnModel, TwoGridMessageColumnModel, TwoGridMessageColumnModel, TwoGridMessageColumnModel,
		TwoGridMessageColumnModel, TwoGridMessageColumnModel, TwoGridMessageColumnModel, TwoGridMessageColumnModel, TwoGridMessageColumnModel,
		TwoGridMessageColumnModel] },
	validate: function(attrs, options) {
		if (!attrs.fixedTitle || attrs.fixedTitle == '') {
			return 'FixedTitle is required';
		}
		
		if (!attrs.columns) {
			return 'Columns is required';
		}
	},
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		return data; 
	}});
var TextWithoutButtonMessageColumnModel = NestedModel.extend({
	name: "TextWithoutButtonMessageColumnModel",
	defaults: { title: "", text: "" },
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		return data;
	}});
var TextWithoutButtonMessageModel = NestedModel.extend({
	name: "TextWithoutButtonMessageModel",
	defaults: { type: "textwithoutbutton", columns: [], fixedTitle: ""},
	model: { columns:[TextWithoutButtonMessageColumnModel, TextWithoutButtonMessageColumnModel, TextWithoutButtonMessageColumnModel, TextWithoutButtonMessageColumnModel,
		TextWithoutButtonMessageColumnModel, TextWithoutButtonMessageColumnModel, TextWithoutButtonMessageColumnModel, TextWithoutButtonMessageColumnModel, TextWithoutButtonMessageColumnModel,
		TextWithoutButtonMessageColumnModel, TextWithoutButtonMessageColumnModel, TextWithoutButtonMessageColumnModel, TextWithoutButtonMessageColumnModel, TextWithoutButtonMessageColumnModel,
		TextWithoutButtonMessageColumnModel] },
	validate: function(attrs, options) {
		if (!attrs.fixedTitle || attrs.fixedTitle == '') {
			return 'FixedTitle is required';
		}
		
		if (!attrs.columns) {
			return 'Columns is required';
		}
	},
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		return data; 
	}});
var TextWithButtonMessageColumnModel = NestedModel.extend({
	name: "TextWithButtonMessageColumnModel",
	defaults: { actions: [], title: "", text: "" },
	model: { actions: [MessageActionModel, MessageActionModel, MessageActionModel, MessageActionModel, MessageActionModel] },
	validate: function(attrs, options) {
		if (attrs.title && attrs.title != '') { // Column.title 是必要，若沒有填視為本 column 是要刪除的
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
		return data;
	}});
var TextWithButtonMessageModel = NestedModel.extend({
	name: "TextWithButtonMessageModel",
	defaults: { type: "threegrid", columns: [], fixedTitle: ""},
	model: { columns:[TextWithButtonMessageColumnModel, TextWithButtonMessageColumnModel, TextWithButtonMessageColumnModel, TextWithButtonMessageColumnModel,
		TextWithButtonMessageColumnModel, TextWithButtonMessageColumnModel, TextWithButtonMessageColumnModel, TextWithButtonMessageColumnModel, TextWithButtonMessageColumnModel,
		TextWithButtonMessageColumnModel, TextWithButtonMessageColumnModel, TextWithButtonMessageColumnModel, TextWithButtonMessageColumnModel, TextWithButtonMessageColumnModel,
		TextWithButtonMessageColumnModel] },
	validate: function(attrs, options) {
		if (!attrs.fixedTitle || attrs.fixedTitle == '') {
			return 'FixedTitle is required';
		}
		
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
var ImageCarouselMessageColumnModel = NestedModel.extend({
	name: "ImageCarouselMessageColumnModel",
	validate: function(attrs, options) {
		if (attrs.imgAltText && attrs.imgAltText != '') { // Column.imgAltText 是必要，若沒有填視為本 column 是要刪除的
			if (!attrs.thumbnailImageUrl) {
				return 'Image is required';
			}
			else if(!attrs.uri) {
				return 'URL is required';
			}
		}
	},
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		//console.log(data);
		return data;
	}});
var ImageCarouselMessageModel = NestedModel.extend({
	name: "ImageCarouselMessageModel",
	defaults: { type: "imageCarousel", columns: [] },
	model: { columns:[ImageCarouselMessageColumnModel, ImageCarouselMessageColumnModel, ImageCarouselMessageColumnModel, ImageCarouselMessageColumnModel, ImageCarouselMessageColumnModel] },
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

var OptionsWithIconTextItemsModel = NestedModel.extend({
	name: "OptionsWithIconTextItemsModel",
	model: { columns:[MessageActionModel, MessageActionModel, MessageActionModel, MessageActionModel]},
	validate: function(attrs, options) {
		if (!attrs.columns) {
			return 'Buttons is required';
		} else if (!attrs.iconText || attrs.iconText == '') {
			return 'Text is required';
		} else if (attrs.extendTitle && attrs.extendTitle != '' && (!attrs.extendContent || attrs.extendContent == '')) {
				return 'Extend content is required';
		} else {
			for (var i = 0 ; i < attrs.columns.length ; i++) {
				col = attrs.columns[i];
				var v = col.validate(col.attributes, options);
				if (typeof(v) != 'undefined') {
					return v;
				}
			}
		}
	},
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		return data;
	}
});

var OptionsWithImageTitleItemsModel = NestedModel.extend({
	name: "OptionsWithImageTitleItemsModel",
	model: { columns:[MessageActionModel, MessageActionModel, MessageActionModel, MessageActionModel]},
	validate: function(attrs, options) {
		if (!attrs.columns) {
			return 'Buttons is required';
		} else if (!attrs.title || attrs.title == '') {
			return 'Title is required';
		} else if (!attrs.thumbnailImageUrl || attrs.thumbnailImageUrl == '') {
			return 'Image is required';
		} else {
			for (var i = 0 ; i < attrs.columns.length ; i++) {
				col = attrs.columns[i];
				var v = col.validate(col.attributes, options);
				if (typeof(v) != 'undefined') {
					return v;
				}
			}
		}
	},
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		return data;
	}
});

var ImageModalItemsModel = NestedModel.extend({
	name: "ImageModalItemsModel",
	model: { columns:[ModalActionModel, ModalActionModel, ModalActionModel, ModalActionModel]},
	validate: function(attrs, options) {
		if (!attrs.columns) {
			return 'Buttons is required';
		} else if (!attrs.thumbnailImageUrl || attrs.thumbnailImageUrl == '') {
			return 'Image is required';
		} else {
			var count = 0;
			var columnLen = attrs.columns.length;
			for (var i = 0 ; i < columnLen ; i++) {
				col = attrs.columns[i];
				var v = col.validate(col.attributes, options);
				if (typeof(v) != 'undefined' && v != 'noLabel') {
					return v;
				}
				if (v == 'noLabel'){
					count += 1;
				}				
			}
			if (count == columnLen) {
				return 'Buttons is required';
			}
		}
	},
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		return data;
	}
});

var TextModalItemsModel = NestedModel.extend({
	name: "TextModalItemsModel",
	model: { columns:[ModalActionModel, ModalActionModel, ModalActionModel, ModalActionModel]},
	validate: function(attrs, options) {
		if (!attrs.columns) {
			return 'Buttons is required';
		} else if (!attrs.title) {
			return 'Title is required';
		} else if (!attrs.text) {
			return 'Text is required';
		} else if (attrs.extendTitle && attrs.extendTitle != '' && (!attrs.extendContent || attrs.extendContent == '')) {
				return 'Extend content is required';
		} else {
			var count = 0;
			var columnLen = attrs.columns.length;
			for (var i = 0 ; i < columnLen ; i++) {
				col = attrs.columns[i];
				var v = col.validate(col.attributes, options);
				if (typeof(v) != 'undefined' && v != 'noLabel') {
					return v;
				}
				if (v == 'noLabel'){
					count += 1;
				}				
			}
			if (count == columnLen) {
				return 'Buttons is required';
			}
		}
	},
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		return data;
	}
});

var AlertItemsModel = NestedModel.extend({
	name: "AlertItemsModel",
	model: { columns:[AlertActionModel, AlertActionModel]},
	validate: function(attrs, options) {		
		if (!attrs.title || attrs.title == '') {
			return 'Title is required';
		} else if (!attrs.text) {
			return 'Text is required';
		} else if (!attrs.columns) {
			return 'Buttons is required';
		} else {
			for (var i = 0 ; i < attrs.columns.length ; i++) {
				col = attrs.columns[i];
				var v = col.validate(col.attributes, options);
				if (typeof(v) != 'undefined') {
					return v;
				}
			}
		}
	},
	parse: function(data) {
		data = commonNestedModelParser(this.model, data);
		return data;
	}
});

var TemplateMessageModel = NestedModel.extend({
	name: "TemplateMessageModel",
	model: {},
	validate: function(attrs, options) {
		//console.log("template message validation");
		if (!attrs.altText || attrs.altText == '') {
			return 'AltText is required';
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
			if (data.template.type == 'buttons') {
				data.template = new ButtonsMessageModel(data.template, {parse: true});
			}
			else if (data.template.type == 'quickReplies') {
				data.template = new QuickRepliesMessageModel(data.template, {parse: true});
			}
			else if (data.template.type == 'carousel') {
				data.template = new CarouselMessageModel(data.template, {parse: true});
			}
			else if (data.template.type == 'imageCarousel') {
				data.template = new ImageCarouselMessageModel(data.template, {parse: true});
			}
			else if (data.template.type == 'stretch') {
				data.template = new StretchMessageModel(data.template, {parse: true});
			}
			else if (data.template.type == 'threegrid') {
				data.template = new ThreeGridMessageModel(data.template, {parse: true});
			}
			else if (data.template.type == 'twogrid') {
				data.template = new TwoGridMessageModel(data.template, {parse: true});
			}
			else if (data.template.type == 'onegrid') {
			  // transfer TemplateMessageModel's template model attr to backbone Model
        data.template = new OneGridMessageModel(data.template, {parse: true});
      }
			else if (data.template.type == 'textwithoutbutton') {
				data.template = new TextWithoutButtonMessageModel(data.template, {parse: true});
			}
			else if (data.template.type == 'textwithbutton') {
				data.template = new TextWithButtonMessageModel(data.template, {parse: true});
			}
		}
		return data;
	}});

var ImagemapMessageModel = NestedModel.extend({
	name: "ImagemapMessageModel",
	defaults: { type: "imagemap", imageMapType: "type1", baseSize: { width: 1040, height: 1040 }, actions: []},
	model: { actions:[ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel, ImagemapActionModel] },  // 30 個
	validate: function(attrs, options) {
		if (!attrs.altText || attrs.altText == '') {
			return 'AltText is required';
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
    		type12: [
    			{ "x": 0, "y": 0, "width": 520, "height": 347 }, 
    			{ "x": 520, "y": 0, "width": 520, "height": 347 }, 
    			{ "x": 0, "y": 347, "width": 520, "height": 347 }, 
    			{ "x": 520, "y": 347, "width": 520, "height": 347 }, 
    			{ "x": 0, "y": 694, "width": 520, "height": 346 }, 
    			{ "x": 520, "y": 694, "width": 520, "height": 346 }, 
    		],
    		type13: [
    			{ "x": 0, "y": 0, "width": 520, "height": 260 },
    			{ "x": 520, "y": 0, "width": 520, "height": 260 },
    			{ "x": 0, "y": 260, "width": 520, "height": 260 },
    			{ "x": 520, "y": 260, "width": 520, "height": 260 },
    			{ "x": 0, "y": 520, "width": 520, "height": 260 },
    			{ "x": 520, "y": 520, "width": 520, "height": 260 },
    			{ "x": 0, "y": 780, "width": 520, "height": 260 },
    			{ "x": 520, "y": 780, "width": 520, "height": 260 },
    		],
    		type14: [
    			{ "x": 0, "y": 0, "width": 347, "height": 208 },
    			{ "x": 347, "y": 0, "width": 347, "height": 208 },
    			{ "x": 694, "y": 0, "width": 346, "height": 208 },
    			{ "x": 0, "y": 208, "width": 347, "height": 208 },
    			{ "x": 347, "y": 208, "width": 347, "height": 208 },
    			{ "x": 694, "y": 208, "width": 346, "height": 208 },
    			{ "x": 0, "y": 416, "width": 347, "height": 208 },
    			{ "x": 347, "y": 416, "width": 347, "height": 208 },
    			{ "x": 694, "y": 416, "width": 346, "height": 208 },
    			{ "x": 0, "y": 624, "width": 347, "height": 208 },
    			{ "x": 347, "y": 624, "width": 347, "height": 208 },
    			{ "x": 694, "y": 624, "width": 346, "height": 208 },
    			{ "x": 0, "y": 832, "width": 347, "height": 208 },
    			{ "x": 347, "y": 832, "width": 347, "height": 208 },
    			{ "x": 694, "y": 832, "width": 346, "height": 208 },
    		],
    		type99: [
    			// type99 代表 free style, 你有 free style 嗎？
    		],
    }
	});

var QuickRepliesMessageModel = NestedModel.extend({
	name: "QuickRepliesMessageModel",
	model: { actions:[MessageActionModel, MessageActionModel, MessageActionModel, MessageActionModel
		, MessageActionModel, MessageActionModel, MessageActionModel, MessageActionModel
		, MessageActionModel, MessageActionModel, MessageActionModel, MessageActionModel, MessageActionModel, MessageActionModel, MessageActionModel] },
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
		data = commonNestedModelParser(this.model, data);
		return data;
}});


var OptionsWithIconTextMessageModel = NestedModel.extend({
	name: "OptionsWithIconTextMessageModel",
	model: { },
	validate: function(attrs, options) {
		if (!attrs.altText || attrs.altText == '') {
			return 'AltText is required';
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
			data.template = new OptionsWithIconTextItemsModel(data.template, {parse: true});
		}
		return data;
	}
});

var OptionsWithImageTitleMessageModel = NestedModel.extend({
	name: "OptionsWithImageTitleMessageModel",
	model: { },
	validate: function(attrs, options) {
		if (!attrs.altText || attrs.altText == '') {
			return 'AltText is required';
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
			data.template = new OptionsWithImageTitleItemsModel(data.template, {parse: true});
		}
		return data;
	}
});

var ImageModalMessageModel = NestedModel.extend({
	name: "ImageModalMessageModel",
	model: { },
	validate: function(attrs, options) {
		if (!attrs.altText || attrs.altText == '') {
			return 'AltText is required';
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
			data.template = new ImageModalItemsModel(data.template, {parse: true});
		}
		return data;
	}
});

var TextModalMessageModel = NestedModel.extend({
	name: "TextModalMessageModel",
	model: { },
	validate: function(attrs, options) {
		if (!attrs.altText || attrs.altText == '') {
			return 'AltText is required';
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
			data.template = new TextModalItemsModel(data.template, {parse: true});
		}
		return data;
	}
});

var AlertMessageModel = NestedModel.extend({
	name: "AlertMessageModel",
	model: { },
	validate: function(attrs, options) {
		if (!attrs.altText || attrs.altText == '') {
			return 'AltText is required';
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
			data.template = new AlertItemsModel(data.template, {parse: true});
		}
		return data;
	}
});

var RichMessageModel = NestedModel.extend({
	name: "RichMessageModel",
	urlRoot: '/wise/wiseadm/webRM/richMessages',  // context path 是個坑？
	defaults: {
		id: null,
		mkey: null,
		tenantId: null,
		updateTime: null,
		createdTime: null,
		expireTime: null,
		msgName: null,
		msgDesc: null,
		msgTemplate: new TemplateMessageModel({ type: "template", template:{ type: "buttons", actions:[]} }, {parse: true}),
		msgType: "buttons"
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
		
		if (attrs.msgTemplate) {
			r = attrs.msgTemplate.validate(attrs.msgTemplate.attributes, options);
			
			if (typeof(r) != 'undefined') {
				return r;
			}
		}
	},
	parse: function(data) {
		if (data.msgType == 'buttons') {
			data.msgTypeName = "Buttons";
			data.msgTemplate = new TemplateMessageModel(data.msgTemplate, {parse: true});
		}
		else if (data.msgType == 'carousel') {
			data.msgTypeName = "Carousel";
			data.msgTemplate = new TemplateMessageModel(data.msgTemplate, {parse: true});
		}
		else if (data.msgType == 'imagemap') {
			data.msgTypeName = data.msgType;
			data.msgTemplate = new ImagemapMessageModel(data.msgTemplate, {parse: true});
		}
		else if (data.msgType == 'quickReplies') {
			data.msgTypeName = data.msgType;
			data.msgTemplate = new TemplateMessageModel(data.msgTemplate, { parse: true});
		}
		else if (data.msgType == 'imageCarousel') {
			data.msgTypeName = '圖片輪播';
			data.msgTemplate = new TemplateMessageModel(data.msgTemplate, {parse: true});
		}
		else if (data.msgType == 'optionsWithIconText') {
			data.msgTypeName = '按鈕';
			data.msgTemplate = new OptionsWithIconTextMessageModel(data.msgTemplate, { parse: true});
		}
		else if (data.msgType == 'optionsWithImageTitle') {
			data.msgTypeName = '圖卡';
			data.msgTemplate = new OptionsWithImageTitleMessageModel(data.msgTemplate, { parse: true});
		}
		else if (data.msgType == 'imageModal') {
			data.msgTypeName = '圖片彈跳視窗';
			data.msgTemplate = new ImageModalMessageModel(data.msgTemplate, { parse: true});
		}
		else if (data.msgType == 'textModal') {
			data.msgTypeName = '文字彈跳視窗';
			data.msgTemplate = new TextModalMessageModel(data.msgTemplate, { parse: true});
		}
		else if (data.msgType == 'alert') {
			data.msgTypeName = '提醒視窗';
			data.msgTemplate = new AlertMessageModel(data.msgTemplate, { parse: true});
		}
		else if (data.msgType == 'text') {
			data.msgTypeName = 'Text';
			data.msgTemplate = new TextMessageModel(data.msgTemplate, { parse: true});
		}
		else if (data.msgType == 'stretch') {
			data.msgTypeName = 'Stretch';
			data.msgTemplate = new TemplateMessageModel(data.msgTemplate, { parse: true});
		}
		else if (data.msgType == 'threegrid') {
			data.msgTypeName = '3 Grid';
			data.msgTemplate = new TemplateMessageModel(data.msgTemplate, { parse: true});
		}
		else if (data.msgType == 'twogrid') {
			data.msgTypeName = '2 Grid';
			data.msgTemplate = new TemplateMessageModel(data.msgTemplate, { parse: true});
		}
	  else if (data.msgType == 'onegrid') {
	    data.msgTypeName = '1 Grid';
	    // transfer RichMessageModel's msgTemplate model attr to backbone Model
	    data.msgTemplate = new TemplateMessageModel(data.msgTemplate, { parse: true}); 
    }
		else if (data.msgType == 'textwithoutbutton') {
			data.msgTypeName = 'Text Without Button';
			data.msgTemplate = new TemplateMessageModel(data.msgTemplate, { parse: true});
		}
		else if (data.msgType == 'textwithbutton') {
			data.msgTypeName = 'Text With Button';
			data.msgTemplate = new TemplateMessageModel(data.msgTemplate, { parse: true});
		}
		return data;
	}
});

var RichMessageCollection = Backbone.Collection.extend({
	url: '/wise/wiseadm/webRM/richMessages',
	model: RichMessageModel,
	parse: function(colData) {
		_.forEach(colData, function(data) {
			data.msgTemplate = JSON.parse(data.msgTemplate);
		});
		return colData;
	},
});
