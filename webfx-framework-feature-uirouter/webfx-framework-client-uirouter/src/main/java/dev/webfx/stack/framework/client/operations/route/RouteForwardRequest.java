package dev.webfx.stack.framework.client.operations.route;

import dev.webfx.stack.framework.shared.operation.HasOperationCode;
import dev.webfx.stack.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.platform.async.AsyncFunction;

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
