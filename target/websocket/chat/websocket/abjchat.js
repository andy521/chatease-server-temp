abjchat = function() {
	if (abjchat.api) {
		return abjchat.api.getInstance.apply(this, arguments);
	}
};

abjchat.version = '0.0.01';

(function(abjchat) {
	var utils = abjchat.utils = {};
    
	utils.exists = function(item) {
		switch (utils.typeOf(item)) {
			case 'string':
				return (item.length > 0);
			case 'object':
				return (item !== null);
			case 'null':
				return false;
		}
		return true;
	};
	
	utils.extend = function() {
		var args = Array.prototype.slice.call(arguments, 0),
			obj = args[0];
		if (args.length > 1) {
			for (var i = 1; i < args.length; i++) {
				utils.foreach(args[i], function(key, val) {
					if (val !== undefined && val !== null) {
						obj[key] = val;
					}
				});
			}
		}
		return obj;
	};
	
	utils.foreach = function(data, fn) {
		for (var key in data) {
			if (data.hasOwnProperty && utils.typeOf(data.hasOwnProperty) === 'function') {
				if (data.hasOwnProperty(key)) {
					fn(key, data[key]);
				}
			} else {
				// IE8 has a problem looping through XML nodes
				fn(key, data[key]);
			}
		}
	};
	
	utils.getCookie = function(key) {
		var arr, reg=new RegExp('(^| )' + key + '=([^;]*)(;|$)');
		if (arr = document.cookie.match(reg))
			return unescape(arr[2]);
		return null;
	};
	
	
	utils.createElement = function(elem, className) {
		var newElement = document.createElement(elem);
		if (className) {
			newElement.className = className;
		}
		return newElement;
	};
	
	utils.addClass = function(element, classes) {
		var originalClasses = utils.typeOf(element.className) === 'string' ? element.className.split(' ') : [];
		var addClasses = utils.typeOf(classes) === 'array' ? classes : classes.split(' ');
		
		utils.foreach(addClasses, function(n, c) {
			if (utils.indexOf(originalClasses, c) === -1) {
				originalClasses.push(c);
			}
		});
		
		element.className = utils.trim(originalClasses.join(' '));
	};
	
	utils.removeClass = function(element, c) {
		var originalClasses = utils.typeOf(element.className) === 'string' ? element.className.split(' ') : [];
		var removeClasses = utils.typeOf(c) === 'array' ? c : c.split(' ');
		
		utils.foreach(removeClasses, function(n, c) {
			var index = utils.indexOf(originalClasses, c);
			if (index >= 0) {
				originalClasses.splice(index, 1);
			}
		});
		
		element.className = utils.trim(originalClasses.join(' '));
	};
	
	utils.emptyElement = function(element) {
		while (element.firstChild) {
			element.removeChild(element.firstChild);
		}
	};
	
	utils.typeOf = function(value) {
		if (value === null || value === undefined) {
			return 'null';
		}
		var typeofString = typeof value;
		if (typeofString === 'object') {
			try {
				if (toString.call(value) === '[object Array]') {
					return 'array';
				}
			} catch (e) {}
		}
		return typeofString;
	};
	
	utils.trim = function(inputString) {
		return inputString.replace(/^\s+|\s+$/g, '');
	};
	
	utils.indexOf = function(array, item) {
		if (array == null) return -1;
		for (var i = 0; i < array.length; i++) {
			if (array[i] === item) {
				return i;
			}
		}
		return -1;
	};
	
	/** Logger */
	var console = window.console = window.console || {
		log: function() {}
	};
	utils.log = function() {
		var args = Array.prototype.slice.call(arguments, 0);
		if (utils.typeOf(console.log) === 'object') {
			console.log(args);
		} else {
			console.log.apply(console, args);
		}
	};
})(abjchat);

(function(abjchat) {
	var utils = abjchat.utils,
		sheet;
	
	function createStylesheet() {
		var styleSheet = document.createElement('style');
		styleSheet.type = 'text/css';
		document.getElementsByTagName('head')[0].appendChild(styleSheet);
		return styleSheet.sheet || styleSheet.styleSheet;
	}
	
	function insertRule(sheet, text, index) {
		try {
			sheet.insertRule(text, index);
		} catch (e) {
			//console.log(e.message, text);
		}
	}
	
	var css = utils.css = function(selector, styles) {
		if (!sheet) {
			sheet = createStylesheet();
		}
		
		var _styles = '';
		utils.foreach(styles, function(style, value) {
			_styles += style + ': ' + value + '; ';
		});
		insertRule(sheet, selector + ' { ' + _styles + '}', (sheet.cssRules || sheet.rules).length);
	};
	
	css.style = function(elements, styles, immediate) {
		if (elements === undefined || elements === null) {
			return;
		}
		if (elements.length === undefined) {
			elements = [elements];
		}
		
		var rules = utils.extend({}, styles);
		for (var i = 0; i < elements.length; i++) {
			var element = elements[i];
			if (element === undefined || element === null) {
				continue;
			}
			
			utils.foreach(rules, function(style, value) {
				var name = getStyleName(style);
				if (element.style[name] !== value) {
					element.style[name] = value;
				}
			});
		}
	};
	
	function getStyleName(name) {
		name = name.split('-');
		for (var i = 1; i < name.length; i++) {
			name[i] = name[i].charAt(0).toUpperCase() + name[i].slice(1);
		}
		return name.join('');
	}
})(abjchat);

(function(abjchat) {
	abjchat.renderModes = {
		DEFAULT: 'def'
	};
})(abjchat);

(function(abjchat) {
	abjchat.states = {
		CONNECTED: 'connected',
		CLOSED: 'closed',
		ERROR: 'error'
	};
})(abjchat);

(function(abjchat) {
	abjchat.events = {
		// General Events
		ERROR: 'ERROR',
		
		// API Events
		ABJCHAT_READY: 'abjchatReady',
		ABJCHAT_SETUP_ERROR: 'abjchatSetupError',
		ABJCHAT_RENDER_ERROR: 'abjchatRenderError',
		
		ABJCHAT_STATE: 'abjchatState',
		ABJCHAT_CONNECT: 'abjchatConnect',
		ABJCHAT_MESSAGE: 'abjchatMessage',
		ABJCHAT_JOIN: 'abjchatJoin',
		ABJCHAT_LEFT: 'abjchatLeft',
		ABJCHAT_CLOSE: 'abjchatClose',
		
		ABJCHAT_VIEW_SEND: 'abjchatViewSend',
		ABJCHAT_VIEW_SHIELDMSG: 'abjchatViewMsgShield',
		ABJCHAT_VIEW_CLEARSCREEN: 'abjchatViewClearScreen',
		ABJCHAT_VIEW_NICKCLICK: 'abjchatViewNickClick'
	};
})(abjchat);

(function(abjchat) {
	var utils = abjchat.utils,
		events = abjchat.events;
	
	events.eventdispatcher = function(id, debug) {
		var _id = id,
			_debug = debug,
			_listeners = {},
			_globallisteners = [];
		
		this.addEventListener = function(type, listener, count) {
			try {
				if (!utils.exists(_listeners[type])) {
					_listeners[type] = [];
				}
				
				if (utils.typeOf(listener) === 'string') {
					listener = (new Function('return ' + listener))();
				}
				_listeners[type].push({
					listener: listener,
					count: count || null
				});
			} catch (err) {
				utils.log('error', err);
			}
			return false;
		};
		
		this.removeEventListener = function(type, listener) {
			if (!_listeners[type]) {
				return;
			}
			try {
				if (listener === undefined) {
					_listeners[type] = [];
					return;
				}
				var i;
				for (i = 0; i < _listeners[type].length; i++) {
					if (_listeners[type][i].listener.toString() === listener.toString()) {
						_listeners[type].splice(i, 1);
						break;
					}
				}
			} catch (err) {
				utils.log('error', err);
			}
			return false;
		};
		
		this.addGlobalListener = function(listener, count) {
			try {
 				if (utils.typeOf(listener) === 'string') {
					listener = (new Function('return ' + listener))();
				}
				_globallisteners.push({
					listener: listener,
					count: count || null
				});
			} catch (err) {
				utils.log('error', err);
			}
			return false;
		};
		
		this.removeGlobalListener = function(listener) {
			if (!listener) {
				return;
			}
			try {
				var i;
				for (i = _globallisteners.length - 1; i >= 0; i--) {
					if (_globallisteners[i].listener.toString() === listener.toString()) {
						_globallisteners.splice(i, 1);
					}
				}
			} catch (err) {
				utils.log('error', err);
			}
			return false;
		};
		
		
		this.dispatchEvent = function(type, data) {
			if (!data) {
				data = {};
			}
			utils.extend(data, {
				id: _id,
				version: abjchat.version,
				type: type
			});
			if (_debug) {
				utils.log(type, data);
			}
			_dispatchEvent(_listeners[type], data, type);
			_dispatchEvent(_globallisteners, data, type);
		};
		
		function _dispatchEvent(listeners, data, type) {
			if (!listeners) {
				return;
			}
			for (var index = 0; index < listeners.length; index++) {
				var listener = listeners[index];
				if (listener) {
					if (listener.count !== null && --listener.count === 0) {
						delete listeners[index];
					}
					try {
						listener.listener(data);
					} catch (err) {
						utils.log('Error handling "' + type +
							'" event listener [' + index + ']: ' + err.toString(), listener.listener, data);
					}
				}
			}
		}
	};
})(abjchat);

(function(abjchat) {
	var utils = abjchat.utils,
		events = abjchat.events;
	
	var _insts = {},
		_eventMapping = {
		onError: events.ERROR,
		onReady: events.ABJCHAT_READY,
		onConnect: events.ABJCHAT_CONNECT,
		onMessage: events.ABJCHAT_MESSAGE,
		onJoin: events.ABJCHAT_JOIN,
		onLeft: events.ABJCHAT_LEFT,
		onNickClick: events.ABJCHAT_VIEW_NICKCLICK,
		onClose: events.ABJCHAT_CLOSE
	};
	
	abjchat.api = function(container) {
		var _this = utils.extend(this, new events.eventdispatcher('api', true)),
			_entity;
		
		_this.container = container;
		_this.id = container.id;
		
		utils.foreach(_eventMapping, function(name, type) {
			_this[name] = function(callback) {
				_this.addEventListener(type, callback);
			};
		});
		
		_this.setup = function(options) {
			utils.emptyElement(_this.container);
			
			_this.config = options;
			_this.embedder = new abjchat.embed(_this);
			_this.embedder.addGlobalListener(_onEvent);
			_this.embedder.embed();
			
			return _this;
		};
		
		_this.setEntity = function(entity, renderMode) {
			_entity = entity;
			_this.renderMode = renderMode;
			
			_this.send = _entity.send;
		};
		
		function _onEvent(e) {
			_forward(e);
		}
		
		function _forward(e) {
			_this.dispatchEvent(e.type, e);
		}
	};
	
	abjchat.api.getInstance = function(identifier) {
		var _container;
		
		if (identifier == null) {
			identifier = 0;
		} else if (identifier.nodeType) {
			_container = identifier;
		} else if (utils.typeOf(identifier) === 'string') {
			_container = document.getElementById(identifier);
		}
		
		if (_container) {
			var inst = _insts[_container.id];
			if (!inst) {
				_insts[identifier] = inst = new abjchat.api(_container);
			}
			return inst;
		} else if (utils.typeOf(identifier) === 'number') {
			return _insts[identifier];
		}
		
		return null;
	};
})(abjchat);

(function(abjchat) {
	abjchat.core = {};
})(abjchat);

(function(abjchat) {
	abjchat.core.renders = {};
})(abjchat);

(function(abjchat) {
	abjchat.core.renders.skins = {};
})(abjchat);

(function(abjchat) {
	var utils = abjchat.utils,
		skins = abjchat.core.renders.skins,
		css = utils.css,
		
		RENDER_CLASS = 'render',
		TITLE_CLASS = 'title',
		MAIN_CLASS = 'main',
		CONSOLE_CLASS = 'console',
		DIALOG_CLASS = 'dialog',
		CONTROL_CLASS = 'control',
		INPUT_CLASS = 'input',
		NICK_SYS_CLASS = 'sys',
		NICK_ME_CLASS = 'me',
		BUTTON_CLASS = 'btn',
		CHECKBOX_CLASS = 'ch',
		
		// For all api instances
		CSS_SMOOTH_EASE = 'opacity .25s ease',
		CSS_100PCT = '100%',
		CSS_ABSOLUTE = 'absolute',
		CSS_RELATIVE = 'relative',
		CSS_NORMAL = 'normal',
		CSS_IMPORTANT = ' !important',
		CSS_HIDDEN = 'hidden',
		CSS_NONE = 'none',
		CSS_BLOCK = 'block',
		
		TITLE_HEIGHT = '34px',
		CONTROL_HEIGHT = '38px';
	
	skins.def = function(config) {
		css('.' + RENDER_CLASS, {
			width: CSS_100PCT,
			height: CSS_100PCT,
			position: CSS_RELATIVE
		});
		
		css(' .' + TITLE_CLASS, {
			padding: 'auto 12px',
			width: CSS_100PCT,
			height: TITLE_HEIGHT,
			
			'font-family': 'inherit',
			'font-size': '14px',
			'font-weight': CSS_NORMAL,
			color: '#fff',
			//opacity: 0.65,
			
			'text-align': 'center',
			'line-height': TITLE_HEIGHT,
			'vertical-align': 'middle',
			
			border: '1px solid #1184ce',
			'border-radius': '4px 4px 0 0',
			'box-shadow': CSS_NONE,
			'background-color': '#1184ce',
			
			cursor: 'not-allowed',
			'pointer-events': CSS_NONE
		});
		css(' .' + TITLE_CLASS + ' span', {
			top: '2px',
			'margin-left': '2px'
		});
		
		css(' .' + MAIN_CLASS, {
			//padding: '0px 15px',
			width: CSS_100PCT,
			/*position: CSS_ABSOLUTE,
			top: TITLE_HEIGHT,
			bottom: '0px',*/
			
			'border-color': '#d6e9c6',
			'box-shadow': '0 1px 1px rgba(0, 0, 0, 0.05)'
		});
		
		css(' .' + MAIN_CLASS + ' .' + CONSOLE_CLASS, {
			width: CSS_100PCT,
			height: (config.height - parseInt(TITLE_HEIGHT)) * 0.75 + 'px',
			'max-height': '75%',
			'overflow-y': 'auto',
			border: '1px solid #1184ce',
			'background-color': '#f8f8f8'
		});
		css(' .' + MAIN_CLASS + ' .' + CONSOLE_CLASS + ' > div', {
			margin: '4px',
			'word-break': 'break-all',
			'word-wrap': 'break-word'
		});
		css(' .' + MAIN_CLASS + ' .' + CONSOLE_CLASS + ' > div > span', {
			'margin-right': '5px'
		});
		css(' .' + MAIN_CLASS + ' .' + CONSOLE_CLASS + ' > div > a', {
			'margin-right': '5px',
			color: '#1184ce',
			'text-decoration': CSS_NONE,
			cursor: 'pointer'
		});
		css(' .' + MAIN_CLASS + ' .' + CONSOLE_CLASS + ' > div > a.' + NICK_SYS_CLASS, {
			font: 'normal bold 14px 微软雅黑,arial,sans-serif',
			color: 'red'
		});
		css(' .' + MAIN_CLASS + ' .' + CONSOLE_CLASS + ' > div > a.' + NICK_ME_CLASS, {
			'margin-right': 0,
			'margin-left': '5px'
		});
		
		css(' .' + MAIN_CLASS + ' .' + DIALOG_CLASS, {
			width: CSS_100PCT,
			position: CSS_RELATIVE
		});
		
		css(' .' + MAIN_CLASS + ' .' + CONTROL_CLASS, {
			width: CSS_100PCT,
			height: CONTROL_HEIGHT,
			border: '1px solid #1184ce',
			'border-width': '0 1px'
		});
		css(' .' + MAIN_CLASS + ' .' + CONTROL_CLASS + ' > div', {
			height: CSS_100PCT,
			'line-height': CONTROL_HEIGHT,
			'text-align': 'center',
			//'vertical-align': 'middle',
			'box-sizing': 'border-box'
		});
		css(' .' + MAIN_CLASS + ' .' + CONTROL_CLASS + ' > div:first-child', {
			'margin-left': '4px'
		});
		css(' .' + MAIN_CLASS + ' .' + CONTROL_CLASS + ' > div:last-child', {
			'margin-right': '4px'
		});
		css(' .' + MAIN_CLASS + ' .' + CONTROL_CLASS + ' > div > .' + CHECKBOX_CLASS, {
			margin: '0px',
			'font-size': '16px',
			'font-weight': CSS_NORMAL,
			color: '#666',
			'vertical-align': 'middle',
			cursor: 'pointer'
		});
		css(' .' + MAIN_CLASS + ' .' + CONTROL_CLASS + ' > div > .' + CHECKBOX_CLASS + ' input[type=checkbox]', {
			cursor: 'pointer'
		});
		css(' .' + MAIN_CLASS + ' .' + CONTROL_CLASS + ' > div > .' + BUTTON_CLASS, {
			padding: '2px 6px',
			color: '#fff',
			'font-size': '14px',
			'font-weight': CSS_NORMAL,
			
			'text-align': 'center',
			'vertical-align': 'middle',
			
			border: '1px solid #1184ce',
			'border-radius': '3px',
			'background-color': '#1184ce',
			cursor: 'pointer'
		});
		css(' .' + MAIN_CLASS + ' .' + CONTROL_CLASS + ' .msgshield', {
			'float': 'left'
		});
		css(' .' + MAIN_CLASS + ' .' + CONTROL_CLASS + ' .clrscreen', {
			'float': 'right'
		});
		css(' .' + MAIN_CLASS + ' .' + CONTROL_CLASS + ' .clrscreen span', {
			top: '2px',
			'margin-left': '2px'
		});
		
		css(' .' + MAIN_CLASS + ' .' + INPUT_CLASS, {
			//'margin-top': '6px',
			width: CSS_100PCT,
			height: ((config.height - parseInt(TITLE_HEIGHT)) * 0.25 - parseInt(CONTROL_HEIGHT)) - 6 + 'px',
			border: '1px solid #1184ce',
			'border-radius': '0 0 4px 4px'
		});
		css(' .' + MAIN_CLASS + ' .' + INPUT_CLASS + ' textarea', {
			'float': 'left',
			margin: '0px',
			padding: '5px 10px',
			width: '84%',
			height: CSS_100PCT,
			resize: CSS_NONE,
			'box-sizing': 'border-box',
			border: '0 none',
			'border-radius': '0 0 0 4px'
		});
		css(' .' + MAIN_CLASS + ' .' + INPUT_CLASS + ' button', {
			'float': 'right',
			padding: 0,
			width: '16%',
			height: CSS_100PCT,
			color: '#fff',
			'box-sizing': 'border-box',
			border: '0 none',
			'background-color': '#1184ce'
		});
	};
})(abjchat);

(function(abjchat) {
	var utils = abjchat.utils,
		events = abjchat.events,
		states = abjchat.states,
		core = abjchat.core,
		renders = core.renders,
		skins = renders.skins,
		css = utils.css,
		
		RENDER_CLASS = 'render',
		TITLE_CLASS = 'title',
		MAIN_CLASS = 'main',
		CONSOLE_CLASS = 'console',
		DIALOG_CLASS = 'dialog',
		CONTROL_CLASS = 'control',
		INPUT_CLASS = 'input',
		NICK_SYS_CLASS = 'sys',
		NICK_ME_CLASS = 'me',
		BUTTON_CLASS = 'btn',
		CHECKBOX_CLASS = 'ch',
		
		// For all api instances
		CSS_SMOOTH_EASE = 'opacity .25s ease',
		CSS_100PCT = '100%',
		CSS_ABSOLUTE = 'absolute',
		CSS_IMPORTANT = ' !important',
		CSS_HIDDEN = 'hidden',
		CSS_NONE = 'none',
		CSS_BLOCK = 'block';
	
	renders.def = function(view, config) {
		var _this = utils.extend(this, new events.eventdispatcher('renders.def', true)),
			_defaults = {
				skin: 'def'
			},
			_container,
			_titleLayer,
			_mainLayer,
			_consoleLayer,
			_dialogLayer,
			_controlLayer,
			_inputLayer,
			
			_titleIcon,
			_textInput,
			_sendButton,
			
			_defaultLayout = '[msgshield][clrscreen]',
			_buttons,
			_skin;
		
		function _init() {
			_this.config = utils.extend({}, _defaults, config);
			
			_container = utils.createElement('div', RENDER_CLASS);
			_titleLayer = utils.createElement('div', TITLE_CLASS);
			_mainLayer = utils.createElement('div', MAIN_CLASS);
			_container.appendChild(_titleLayer);
			_container.appendChild(_mainLayer);
			
			_consoleLayer = utils.createElement('div', CONSOLE_CLASS);
			_dialogLayer = utils.createElement('div', DIALOG_CLASS);
			_mainLayer.appendChild(_consoleLayer);
			_mainLayer.appendChild(_dialogLayer);
			
			_controlLayer = utils.createElement('div', CONTROL_CLASS);
			_inputLayer = utils.createElement('div', INPUT_CLASS);
			_dialogLayer.appendChild(_controlLayer);
			_dialogLayer.appendChild(_inputLayer);
			
			_titleLayer.innerHTML = '聊天室';
			_titleIcon = utils.createElement('span', 'glyphicon glyphicon-envelope');
			_titleLayer.appendChild(_titleIcon);
			
			_buildComponents();
			
			_textInput = utils.createElement('textarea');
			_textInput.setAttribute('placeholder', '输入聊天内容');
			_textInput.setAttribute('maxlength', '30');
			try {
				_textInput.addEventListener('keypress', _onKeyPress);
			} catch(e) {
				_textInput.attachEvent('onkeypress', _onKeyPress);
			}
			_inputLayer.appendChild(_textInput);
			_sendButton = utils.createElement('button');
			_sendButton.innerHTML = '发送';
			try {
				_sendButton.addEventListener('click', _onClick);
			} catch(e) {
				_sendButton.attachEvent('onclick', _onClick);
			}
			_inputLayer.appendChild(_sendButton);
			
			try {
				_skin = new skins[_this.config.skin](_this.config);
			} catch (e) {
				utils.log('Skin [' + _this.config.skin + '] not found.');
			}
			if (!_skin) {
				_this.dispatchEvent(events.ABJCHAT_RENDER_ERROR, { message: 'No suitable skin found!', skin: _this.config.skin });
				return;
			}
		}
		
		function _buildComponents() {
			_addCheckBox('msgshield', events.ABJCHAT_VIEW_SHIELDMSG, null, '屏蔽消息', false);
			_addButton('clrscreen', events.ABJCHAT_VIEW_CLEARSCREEN, null, '清屏', 'glyphicon glyphicon-trash');
		}
		
		function _addCheckBox(name, event, data, label, checked) {
			var box = utils.createElement('div', name);
			var lb = utils.createElement('label', CHECKBOX_CLASS);
			var ch = utils.createElement('input');
			ch.type = 'checkbox';
			ch.checked = !!checked;
			try {
				ch.addEventListener('change', function(e) {
					_this.dispatchEvent(event, utils.extend({ shield: ch.checked }, data));
				});
			} catch(e) {
				ch.attachEvent('onchange', function(e) {
					_this.dispatchEvent(event, utils.extend({ shield: ch.checked }, data));
				});
			}
			lb.appendChild(ch);
			lb.insertAdjacentHTML('beforeend', label);
			box.appendChild(lb);
			_controlLayer.appendChild(box);
		}
		
		function _addButton(name, event, data, label, iconclass) {
			var box = utils.createElement('div', name);
			var btn = utils.createElement('a', BUTTON_CLASS);
			btn.innerHTML = label;
			try {
				btn.addEventListener('click', function(e) {
					_this.dispatchEvent(event, data);
				});
			} catch(e) {
				btn.attachEvent('onclick', function(e) {
					_this.dispatchEvent(event, data);
				});
			}
			
			var btnIcon = utils.createElement('span', iconclass);
			btn.appendChild(btnIcon);
			box.appendChild(btn);
			_controlLayer.appendChild(box);
		}
		
		_this.show = function(data, user) {
			var box = utils.createElement('div');
			
			var message;
			switch (utils.typeOf(data)) {
				case 'object':
					message = data.text;
					if (data.type == 'uni') {
						var span = utils.createElement('span');
						span.innerHTML = '[密语]';
						box.appendChild(span);
					}
					break;
				default:
					message = data;
			}
			
			switch (utils.typeOf(user)) {
				case 'string':
					if (user === '') break;
					user = { id: 0, name: user, role: 0 }; // fall through
				case 'null':
					user = { id: 0, name: '[系统]', role: 128 }; // fall through
				case 'object':
					if (utils.typeOf(user.id) == null) break;
					if (user.id > 0 && utils.typeOf(user.role) != null && user.role > 0) {
						var icon = utils.createElement('span', 'v' + user.role);
						icon.innerHTML = 'v' + user.role;
						box.appendChild(icon);
					}
					
					var a = utils.createElement('a', user.id == '0' ? NICK_SYS_CLASS : (user.id == 'abj' ? NICK_ME_CLASS : ''));
					a.user = utils.extend({}, user);
					a.innerHTML = user.name;
					try {
						a.addEventListener('click', function(e) {
							_this.dispatchEvent(events.ABJCHAT_VIEW_NICKCLICK, { user: this.user });
						});
					} catch(e) {
						a.attachEvent('onclick', function(e) {
							_this.dispatchEvent(events.ABJCHAT_VIEW_NICKCLICK, { user: this.user });
						});
					}
					box.appendChild(a);
					break;
			}
			
			box.insertAdjacentHTML(user && user.id == 'abj' ? 'afterbegin' : 'beforeend', message);
			
			if (_consoleLayer.childNodes.length >= _this.config.maxlog) {
				_consoleLayer.removeChild(_consoleLayer.childNodes[0]);
			}
			_consoleLayer.appendChild(box);
			_consoleLayer.scrollTop = _consoleLayer.scrollHeight;
		};
		
		function _onKeyPress(event) {
			var e = window.event || event;
			if (e.keyCode != 13){
				return;
			}
			
			if (e.ctrlKey){
				_textInput.value += '\r\n';
			} else {
				_this.send();
				
				if (window.event) {
					window.event.returnValue = false;
				} else {
					e.preventDefault();
				}
			}
		}
		
		function _onClick(e) {
			_this.send();
		}
		
		_this.send = function() {
			_this.dispatchEvent(events.ABJCHAT_VIEW_SEND, { message: _textInput.value, userId: null });
			_this.clearInput();
		}
		
		_this.clearInput = function() {
			_textInput.value = '';
		};
		
		_this.clearScreen = function() {
			utils.emptyElement(_consoleLayer);
		};
		
		_this.element = function() {
			return _container;
		};
		
		_this.destroy = function() {
			
		};
		
		_init();
	};
})(abjchat);

(function(abjchat) {
	var utils = abjchat.utils,
		events = abjchat.events,
		core = abjchat.core;
	
	core.entity = function(config) {
		var _this = utils.extend(this, new events.eventdispatcher('core.entity', true)),
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
			_this.initializeAPI = _initializeAPI;
		}
		
		_this.setup = function() {
			_view.setup();
		};
		
		function _forward(e) {
			_this.dispatchEvent(e.type, e);
		}
		
		function _initializeAPI() {
			_this.send = _controller.send;
			_this.resize = _view.resize;
			
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
		}
		
		_init();
	};
})(abjchat);

(function(abjchat) {
	var utils = abjchat.utils,
		events = abjchat.events,
		states = abjchat.states,
		core = abjchat.core;
	
	core.model = function(config) {
		 var _this = utils.extend(this, new events.eventdispatcher('core.model', true)),
		 	_defaults = {};
		
		function _init() {
			_this.config = utils.extend({}, _defaults, config);
			utils.extend(_this, {
				id: config.id,
				userId: 'abj',
				state: states.CLOSED,
				shieldMsg: false
			}, _this.config);
		}
		
		_this.setState = function(state) {
			if (state === _this.state) {
				return;
			}
			_this.state = state;
			_this.dispatchEvent(events.ABJCHAT_STATE, { state: state });
		};
		
		_this.setMsgShield = function(shield) {
			if (shield === _this.shieldMsg) {
				return;
			}
			_this.shieldMsg = shield;
			_this.dispatchEvent(events.ABJCHAT_VIEW_SHIELDMSG, { shield: shield });
		};
		
		_this.getConfig = function(name) {
			return _this.config[name] || {};
		};
		
		_this.destroy = function() {
			
		};
		
		_init();
    };
})(abjchat);

(function(abjchat) {
	var utils = abjchat.utils,
		events = abjchat.events,
		renderModes = abjchat.renderModes,
		states = abjchat.states,
		embed = abjchat.embed,
		core = abjchat.core,
		renders = core.renders,
		css = utils.css,
		
		WRAP_CLASS = 'wrap';
	
	core.view = function(entity, model) {
		var _this = utils.extend(this, new events.eventdispatcher('core.view', true)),
			_wrapper,
			_render,
			_errorState = false;
		
		function _init() {
			_wrapper = utils.createElement('div', WRAP_CLASS + ' col-xs-3 col-sm-3 col-md-3 messageDiv');
			_wrapper.id = entity.id;
			_wrapper.tabIndex = 0;
			
			_this.resize(model.width, model.height);
			
			var replace = document.getElementById(entity.id);
			replace.parentNode.replaceChild(_wrapper, replace);
		}
		
		_this.setup = function() {
			_setupRender();
			try {
				_wrapper.addEventListener('keydown', _onKeyDown);
			} catch(e) {
				_wrapper.attachEvent('onkeydown', _onKeyDown);
			}
			
			css('.' + WRAP_CLASS, {
				width: model.width + 'px',
				height: model.height + 'px'
			});
			
			setTimeout(function() {
				_this.resize(model.width, model.height);
				_this.dispatchEvent(events.ABJCHAT_READY, { channelId: entity.id });
			}, 0);
		};
		
		function _setupRender() {
			switch (model.renderMode) {
				case renderModes.DEFAULT:
					var renderConf = utils.extend(model.getConfig('render'), { width: model.config.width, height: model.config.height });
					_this.render = _render = new renders[renderModes.DEFAULT](_this, renderConf);
					break;
				default:
					_this.dispatchEvent(events.ABJCHAT_SETUP_ERROR, { message: 'Unknown render mode!', render: model.renderMode });
					break;
			}
			
			if (_render) {
				_render.addEventListener(events.ABJCHAT_VIEW_SEND, _onSend);
				_render.addEventListener(events.ABJCHAT_VIEW_SHIELDMSG, _onShieldMsg);
				_render.addEventListener(events.ABJCHAT_VIEW_CLEARSCREEN, _onClearScreen);
				_render.addEventListener(events.ABJCHAT_VIEW_NICKCLICK, _onNickClick);
				_render.addEventListener(events.ABJCHAT_RENDER_ERROR, _onRenderError);
				_wrapper.appendChild(_render.element());
			}
		}
		
		function _onSend(e) {
			_forward(e);
		}
		
		function _onShieldMsg(e) {
			_forward(e);
		}
		
		function _onClearScreen(e) {
			_forward(e);
		}
		
		function _onNickClick(e) {
			_forward(e);
		}
		
		function _onRenderError(e) {
			_forward(e);
		}
		
		function _forward(e) {
			_this.dispatchEvent(e.type, e);
		}
		
		function _onKeyDown(e) {
			if (e.ctrlKey || e.metaKey) {
				return true;
			}
			
			switch (e.keyCode) {
				case 13: // enter
					_render.send();
					break;
				default:
					break;
			}
			
			if (/13/.test(e.keyCode)) {
				// Prevent keypresses from scrolling the screen
				e.preventDefault ? e.preventDefault() : e.returnValue = false;
				return false;
			}
		}
		
		_this.show = function(message, user) {
			if (model.shieldMsg) {
				return;
			}
			if (_render) {
				_render.show(message, user);
			}
		};
		
		_this.resize = function(width, height) {
			
		};
		
		_this.destroy = function() {
			if (_wrapper) {
				_wrapper.removeEventListener('keydown', _onKeyDown);
			}
			if (_render) {
				_render.destroy();
			}
		};
		
		_init();
	};
})(abjchat);

(function(abjchat) {
	var utils = abjchat.utils,
		events = abjchat.events,
		states = abjchat.states,
		core = abjchat.core;
	
	core.controller = function(model, view) {
		var _this = utils.extend(this, new events.eventdispatcher('core.controller', model.config.debug)),
			_ready = false,
			_websocket;
		
		function _init() {
			model.addEventListener(events.ABJCHAT_STATE, _modelStateHandler);
			model.addEventListener(events.ABJCHAT_VIEW_SHIELDMSG, _onModelShieldMsg);
			
			view.addEventListener(events.ABJCHAT_READY, _onReady);
			view.addEventListener(events.ABJCHAT_VIEW_SEND, _onSend);
			view.addEventListener(events.ABJCHAT_VIEW_SHIELDMSG, _onViewShieldMsg);
			view.addEventListener(events.ABJCHAT_VIEW_CLEARSCREEN, _onClearScreen);
			view.addEventListener(events.ABJCHAT_VIEW_NICKCLICK, _onNickClick);
			view.addEventListener(events.ABJCHAT_SETUP_ERROR, _onSetupError);
			view.addEventListener(events.ABJCHAT_RENDER_ERROR, _onRenderError);
		}
		
		_this.send = function(message, userId) {
			if (!_websocket || model.state == states.CLOSED) {
				_connect();
				return;
			}
			
			_websocket.send(message);
		};
		
		function _connect() {
			if (!_websocket) {
				if (window.WebSocket) {
					_websocket = new WebSocket(model.url + '?token=' + utils.getCookie('token'));
				} else if (window.MozWebSocket) {
					_websocket = new MozWebSocket(model.url + '?token=' + utils.getCookie('token'));
				} else {
					_websocket = new SockJS(model.url.replace(/^ws/, 'http') + '/sockjs');
				}
				_websocket.onopen = function(e) {
					utils.log('websocket.onopen');
					model.setState(states.CONNECTED);
				};
				_websocket.onmessage = function(e) {
					utils.log('websocket.onmessage: ' + e.data);
					var data = eval('(' + e.data + ')');
					switch (data.raw) {
						case 'message':
							view.show(data.data, data.user);
							_this.dispatchEvent(events.ABJCHAT_MESSAGE, data);
							break;
						case 'join':
							view.show('joined.', data.user);
							_this.dispatchEvent(events.ABJCHAT_JOIN, data);
							break;
						case 'left':
							view.show('left.', data.user);
							_this.dispatchEvent(events.ABJCHAT_LEFT, data);
							break;
						default:
							utils.log('Unknown data type, ignored.');
							break;
					}
				};
				_websocket.onerror = function(e) {
					utils.log('websocket.onerror');
					model.setState(states.ERROR);
				};
				_websocket.onclose = function(e) {
					utils.log('websocket.onclose');
					model.setState(states.CLOSED);
				};
			}
		}
		
		function _modelStateHandler(e) {
			switch (e.state) {
				case states.CONNECTED:
					view.show('Chat room connected.');
					_this.dispatchEvent(events.ABJCHAT_CONNECT, { channelId: model.id });
					break;
				case states.CLOSED:
					view.show('Chat room closed.');
					_this.dispatchEvent(events.ABJCHAT_CLOSE, { channelId: model.id });
					break;
				case states.ERROR:
					view.show('Chat room error!');
					_this.dispatchEvent(events.ERROR, { message: 'Chat room error!', channelId: model.id });
					break;
				default:
					_this.dispatchEvent(events.ERROR, { message: 'Unknown model state!', state: e.state });
					break;
			}
		}
		
		function _onModelShieldMsg(e) {
			_forward(e);
		}
		
		function _onReady(e) {
			if (!_ready) {
				_forward(e);
				
				//model.addGlobalListener(_forward);
				//view.addGlobalListener(_forward);
				
				_ready = true;
				_connect();
				
				window.onbeforeunload = function(e) {
					if (_websocket && model.state == states.CONNECTED) {
						_websocket.close();
					}
				};
			}
		}
		
		function _onSend(e) {
			_this.send(e.message, e.userId);
		}
		
		function _onViewShieldMsg(e) {
			model.setMsgShield(e.shield);
		}
		
		function _onClearScreen(e) {
			view.render.clearScreen();
			_forward(e);
		}
		
		function _onNickClick(e) {
			_forward(e);
		}
		
		function _onSetupError(e) {
			_forward(e);
		}
		
		function _onRenderError(e) {
			_forward(e);
		}
		
		function _forward(e) {
			_this.dispatchEvent(e.type, e);
		}
		
		_this.close = function() {
			if (_websocket)
				_websocket.close();
		};
		
		_init();
	};
})(abjchat);

(function(abjchat) {
	var utils = abjchat.utils,
		renderModes = abjchat.renderModes,
		events = abjchat.events;
	
	var embed = abjchat.embed = function(api) {
		var _this = utils.extend(this, new events.eventdispatcher('embed', true)),
			_config = new embed.config(api.config),
			_errorOccurred = false,
			_embedder = null;
		_config.id = api.id;
		
		utils.foreach(_config.events, function(e, cb) {
			var fn = api[e];
			if (utils.typeOf(fn) === 'function') {
				fn.call(api, cb);
			}
		});
		
		_this.embed = function() {
			try {
				_embedder = new embed[_config.renderMode](api, _config);
			} catch (e) {
				utils.log('Render [' + _config.renderMode + '] not found.');
			}
			
			if (!_embedder || !_embedder.supports()) {
				if (_config.fallback) {
					_config.renderMode = _config.renderMode = renderModes.DEFAULT;
					_embedder = new embed.def(api, _config);
				} else {
					_this.dispatchEvent(events.ABJCHAT_SETUP_ERROR, { message: 'No suitable render found!', render: _config.renderMode, fallback: _config.fallback });
					return;
				}
			}
			_embedder.addGlobalListener(_onEvent);
			_embedder.embed();
		};
		
		_this.errorScreen = function(message) {
			if (_errorOccurred) {
				return;
			}
			
			_errorOccurred = true;
			_this.errorScreen(api.container, e.message, _config);
		};
		
		function _onEvent(e) {
			switch (e.type) {
				case events.ERROR:
				case events.ABJCHAT_SETUP_ERROR:
				case events.ABJCHAT_RENDER_ERROR:
					_this.errorScreen(e.message);
					break;
				default:
					break;
			}
			_forward(e);
		}
		
		function _forward(e) {
			_this.dispatchEvent(e.type, e);
		}
		
		return _this;
	};
	
	embed.errorScreen = function(container, message, config) {
		// Do nothing here
	};
})(abjchat);

(function(abjchat) {
	var utils = abjchat.utils,
		events = abjchat.events,
		renderModes = abjchat.renderModes,
		embed = abjchat.embed;
	
	embed.config = function(config) {
		var _defaults = {
			url: window.location.host + (window.location.href.indexOf('RCP') == -1 ? '' : '/RCP'),
			width: 300,
			height: 450,
	 		renderMode: renderModes.DEFAULT,
			fallback: true
		},
		_config = utils.extend({}, _defaults, config);
		
		return _config;
	};
	
	embed.config.addConfig = function(oldConfig, newConfig) {
		return utils.extend(oldConfig, newConfig);
	};
})(abjchat);

(function(abjchat) {
	var utils = abjchat.utils,
		events = abjchat.events,
		renderModes = abjchat.renderModes,
		embed = abjchat.embed,
		core = abjchat.core;
	
	embed.def = function(api, config) {
		var _this = utils.extend(this, new events.eventdispatcher('embed.def', true));
		_this.renderMode = renderModes.DEFAULT;
		
		_this.embed = function() {
			var entity = new core.entity(config);
			entity.addGlobalListener(_onEvent);
			entity.setup();
			api.setEntity(entity, config.renderMode);
		};
		
		function _onEvent(e) {
			_forward(e);
		}
		
		function _forward(e) {
			_this.dispatchEvent(e.type, e);
		}
        
		_this.supports = function() {
            return true;
        };
	};
})(abjchat);




(function(abjchat) {
	
})(abjchat);

(function(abjchat) {
	
})(abjchat);
