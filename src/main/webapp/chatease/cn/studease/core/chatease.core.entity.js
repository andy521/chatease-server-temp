﻿(function(chatease) {
	var utils = chatease.utils,
		events = chatease.events,
		core = chatease.core;
	
	core.entity = function(config) {
		var _this = utils.extend(this, new events.eventdispatcher('core.entity')),
			_model,
			_view,
			_controller;
		
		function _init() {
			_this.id = config.id;
			_this.model = _model = new core.model(config);
			_this.view = _view = new core.view(_this, _model);
			_this.controller = _controller = new core.controller(_model, _view);
			_controller.addGlobalListener(_forward);
			
			_initializeAPI();
		}
		
		function _initializeAPI() {
			_this.send = _controller.send;
			_this.join = _controller.join;
			_this.resize = _view.resize;
		}
		
		_this.setup = function() {
			_view.setup();
		};
		
		function _forward(e) {
			_this.dispatchEvent(e.type, e);
		}
		
		_this.destroy = function() {
			if (_controller) {
				_controller.stop();
			}
			if (_view) {
				_view.destroy();
			}
			if (_model) {
				_model.destroy();
			}
		};
		
		_init();
	};
})(chatease);
