package dev.webfx.stack.com.websocket.spi.impl.gwtj2cl.sockjs;

import elemental2.dom.Event;
import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * @author Bruno Salmon
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL)
public final class TransportMessageEvent extends Event {

    @JsConstructor
    public TransportMessageEvent(String type) {
        super(type);
    }

    public String data;

}
