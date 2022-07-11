package dev.webfx.stack.com.buscall.spi;

import dev.webfx.stack.async.AsyncFunction;

/**
 * @author Bruno Salmon
 */
public class AsyncFunctionBusCallEndpoint<A, R> extends BusCallEndPointBase<A, R> {

    public AsyncFunctionBusCallEndpoint(String address, AsyncFunction<A, R> asyncFunction) {
        super(address, asyncFunction);
    }
}
