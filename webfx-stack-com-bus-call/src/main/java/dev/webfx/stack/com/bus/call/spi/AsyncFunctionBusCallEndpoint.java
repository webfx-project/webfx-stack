package dev.webfx.stack.com.bus.call.spi;

import dev.webfx.platform.async.AsyncFunction;

/**
 * @author Bruno Salmon
 */
public class AsyncFunctionBusCallEndpoint<A, R> extends BusCallEndPointBase<A, R> {

    public AsyncFunctionBusCallEndpoint(String address, AsyncFunction<A, R> asyncFunction) {
        super(address, asyncFunction);
    }
}
