package dev.webfx.stack.com.websocket.spi.impl.gwt;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;


/**
 * @author Bruno Salmon
 */
@JsType(isNative = true, namespace = "window")
final class SockJS {

    @JsConstructor
    public SockJS(String url, Object ignored, ReadOnlyAstObject options) {}

    public native void send(String data);

    public native void close();

    @JsProperty(name = "readyState")
    public native Object readyState();

    @JsProperty(name = "onopen")
    public native void setOnOpen(JSFunction<Void> listener);
    @JsProperty(name = "onmessage")
    public native void setOnMessage(JSFunction<SockJSEvent> listener);
    @JsProperty(name = "onclose")
    public native void setOnClose(JSFunction<ReadOnlyAstObject> listener);
    @JsProperty(name = "onerror")
    public native void setOnError(JSFunction<SockJSEvent> listener);

    @JsProperty(name = "OPEN")
    public native static Object OPEN();
    @JsProperty(name = "CONNECTING")
    public native static Object CONNECTING();
    @JsProperty(name = "CLOSING")
    public native static Object CLOSING();
    @JsProperty(name = "CLOSED")
    public native static Object CLOSED();

}
