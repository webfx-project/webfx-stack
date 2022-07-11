package dev.webfx.stack.com.buscall.spi;

import dev.webfx.stack.async.AsyncFunction;

/**
 * @author Bruno Salmon
 */
public interface BusCallEndpoint<A, R> {

    String getAddress();

    AsyncFunction<A, R> toAsyncFunction();

}
