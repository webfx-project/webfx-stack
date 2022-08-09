package dev.webfx.stack.com.bus.call.spi;

import dev.webfx.platform.async.AsyncFunction;

/**
 * @author Bruno Salmon
 */
public interface BusCallEndpoint<A, R> {

    String getAddress();

    AsyncFunction<A, R> toAsyncFunction();

}
