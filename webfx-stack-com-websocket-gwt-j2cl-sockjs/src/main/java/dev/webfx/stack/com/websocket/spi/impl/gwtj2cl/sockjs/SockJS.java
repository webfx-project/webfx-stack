package dev.webfx.stack.com.websocket.spi.impl.gwtj2cl.sockjs;

import elemental2.dom.Event;
import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;


/**
 * @author Bruno Salmon
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL)
final class SockJS {

    public static Object OPEN;
    public static Object CONNECTING;
    public static Object CLOSING;
    public static Object CLOSED;

    @JsConstructor
    public SockJS(String url, Object ignored, Object options) {}

    public native void send(String data);
    public native void close();
    public Object readyState;
    public OnopenFn onopen;
    public OnmessageFn onmessage;
    public OncloseFn onclose;
    public OnerrorFn onerror;

    @JsFunction
    public interface OnopenFn {
        void onInvoke(Event e);
    }

    @JsFunction
    public interface OnmessageFn {
        void onInvoke(TransportMessageEvent e);
    }

    @JsFunction
    public interface OncloseFn {
        void onInvoke(Event e);
    }

    @JsFunction
    public interface OnerrorFn {
        void onInvoke(TransportMessageEvent e);
    }
}
