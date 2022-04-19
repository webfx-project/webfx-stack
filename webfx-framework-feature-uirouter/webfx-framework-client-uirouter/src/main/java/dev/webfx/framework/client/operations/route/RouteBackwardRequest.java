package dev.webfx.framework.client.operations.route;

import dev.webfx.framework.shared.operation.HasOperationCode;
import dev.webfx.platform.client.services.windowhistory.spi.BrowsingHistory;
import dev.webfx.platform.shared.async.AsyncFunction;

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
