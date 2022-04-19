package dev.webfx.platform.shared.services.buscall.spi;

import dev.webfx.platform.shared.async.AsyncFunction;

/**
 * @author Bruno Salmon
 */
public interface BusCallEndpoint<A, R> {

    String getAddress();

    AsyncFunction<A, R> toAsyncFunction();

}
