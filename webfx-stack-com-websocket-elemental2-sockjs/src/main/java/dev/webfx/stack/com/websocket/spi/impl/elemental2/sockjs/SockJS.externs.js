/**
 * @externs
 */

/**
 * @constructor
 */
var SockJS = function(url, ignored, options) {

    /**
     * @type {Object}
     */
    this.readyState = null;

    /**
     * @type {null|function(Event): undefined}
     */
    this.onopen = null;

    /**
     * @type {null|function(TransportMessageEvent): undefined}
     */
    this.onmessage = null;

    /**
     * @type {null|function(Event): undefined}
     */
    this.onclose = null;

    /**
     * @type {null|function(TransportMessageEvent): undefined}
     */
    this.onerror = null;
};

SockJS.CONNECTING = {};
SockJS.OPEN = {};
SockJS.CLOSING = {};
SockJS.CLOSED = {};

SockJS.prototype.send = function(data) {};
SockJS.prototype.close = function() {};
