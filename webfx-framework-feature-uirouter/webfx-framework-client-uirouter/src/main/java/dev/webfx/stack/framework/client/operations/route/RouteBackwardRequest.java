package dev.webfx.stack.framework.client.operations.route;

import dev.webfx.stack.framework.shared.operation.HasOperationCode;
import dev.webfx.stack.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.async.AsyncFunction;

/**
 * @author Bruno Salmon
 */
public final class RouteBackwardRequest
        extends RouteRequestBase<RouteBackwardRequest>
        implements HasOperationCode {

    private static final String OPERATION_CODE = "RouteBackward";

    public RouteBackwardRequest(BrowsingHistory history) {
        super(history);
    }

    @Override
    public Object getOperationCode() {
        return OPERATION_CODE;
    }

    @Override
    public AsyncFunction<RouteBackwardRequest, Void> getOperationExecutor() {
        return RouteBackwardExecutor::executeRequest;
    }

}
