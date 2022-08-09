package dev.webfx.stack.com.bus.call.spi;

import dev.webfx.platform.async.Future;

import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public class FunctionBusCallEndpoint<A, R> extends BusCallEndPointBase<A, R> {

    public FunctionBusCallEndpoint(String address, Function<A, R> function) {
        super(address, arg -> Future.succeededFuture(function.apply(arg)));
    }
}
