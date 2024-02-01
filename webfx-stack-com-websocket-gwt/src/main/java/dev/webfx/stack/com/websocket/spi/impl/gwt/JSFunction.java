package dev.webfx.stack.com.websocket.spi.impl.gwt;

import jsinterop.annotations.JsFunction;

/**
 * @author Bruno Salmon
 */
@JsFunction
@FunctionalInterface
interface JSFunction<T> {

    void apply(T t);

}
