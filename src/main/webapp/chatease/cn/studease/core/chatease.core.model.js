﻿(function(chatease) {
	var utils = chatease.utils,
		events = chatease.events,
		core = chatease.core,
		states = core.states;
	
	core.model = function(config) {
		 var _this = utils.extend(this, new events.eventdispatcher('core.model')),
		 	_defaults = {},
		 	_attributes = {};
		
		function _init() {
			_this.config = utils.extend({}, _defaults, config);
			utils.extend(_this, {
				id: config.id,
				user: {
					id: NaN,
					name: ''
				},
				state: states.CLOSED,
				shieldMsg: false
			}, _this.config);
		}
		
		_this.setState = function(state) {
			if (state === _this.state) {
				return;
			}
			_this.state = state;
			_this.dispatchEvent(events.CHATEASE_STATE, { state: state });
		};
		
		_this.setMsgShield = function(shield) {
			if (shield === _this.shieldMsg) {
				return;
			}
			_this.shieldMsg = shield;
			_this.dispatchEvent(events.CHATEASE_VIEW_SHIELDMSG, { shield: shield });
		};
		
		_this.getConfig = function(name) {
			return _this.config[name] || {};
		};
		
		_this.getAttributes = function(channelId) {
			if (_attributes.hasOwnProperty(channelId) == false) {
				_attributes[channelId] = new core.userattributes(_this, channelId);
			}
			return _attributes[channelId];
		};
		
		_this.destroy = function() {
			
		};
		
		_init();
    };
})(chatease);
