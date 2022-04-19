package dev.webfx.framework.client.operations.route;

import dev.webfx.framework.shared.operation.HasOperationCode;
import dev.webfx.platform.client.services.windowhistory.spi.BrowsingHistory;
import dev.webfx.platform.shared.async.AsyncFunction;

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
