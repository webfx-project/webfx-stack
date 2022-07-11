package dev.webfx.stack.framework.client.operations.route;

import dev.webfx.stack.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.platform.json.JsonObject;
import dev.webfx.stack.async.AsyncFunction;

/**
 * @author Bruno Salmon
 */
public class RoutePushRequest extends RouteRequestBase<RoutePushRequest> {

    private JsonObject state;

    public RoutePushRequest(String routePath, BrowsingHistory browsingHistory) {
        this(routePath, browsingHistory, null);
    }

    public RoutePushRequest(String routePath, BrowsingHistory history, JsonObject state) {
        super(routePath, history);
        this.state = state;
    }

    public JsonObject getState() {
        return state;
    }

    public RoutePushRequest setState(JsonObject state) {
        this.state = state;
        return this;
    }

    @Override
    public AsyncFunction<RoutePushRequest, Void> getOperationExecutor() {
        return RoutePushExecutor::executePushRouteRequest;
    }
}
