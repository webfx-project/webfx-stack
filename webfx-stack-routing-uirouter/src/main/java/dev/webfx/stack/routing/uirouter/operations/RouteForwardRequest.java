package dev.webfx.stack.routing.uirouter.operations;

import dev.webfx.stack.ui.operation.HasOperationCode;
import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.platform.async.AsyncFunction;

/**
 * @author Bruno Salmon
 */
public final class RouteForwardRequest
        extends RouteRequestBase<RouteForwardRequest>
        implements HasOperationCode {

    private static final String OPERATION_CODE = "RouteForward";

    public RouteForwardRequest(BrowsingHistory history) {
        super(history);
    }

    @Override
    public Object getOperationCode() {
        return OPERATION_CODE;
    }

    @Override
    public AsyncFunction<RouteForwardRequest, Void> getOperationExecutor() {
        return RouteForwardExecutor::executeRequest;
    }

}
