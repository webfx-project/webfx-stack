package dev.webfx.stack.com.buscall.spi;

import dev.webfx.platform.util.function.Callable;

/**
 * @author Bruno Salmon
 */
public final class CallableBusCallEndpoint<R> extends FunctionBusCallEndpoint<Object, R> {

    public CallableBusCallEndpoint(String address, Callable<R> callable) {
        super(address, ignored -> callable.call());
    }
}
