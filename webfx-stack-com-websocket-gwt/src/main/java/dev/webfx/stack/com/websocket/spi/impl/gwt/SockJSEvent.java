package dev.webfx.stack.com.websocket.spi.impl.gwt;

import elemental2.dom.Event;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * @author Bruno Salmon
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL)
public final class SockJSEvent extends Event {

    public SockJSEvent(String type) {
        super(type);
    }

    public String data;

}
