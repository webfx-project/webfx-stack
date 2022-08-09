package dev.webfx.stack.routing.uirouter.operations;

import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.platform.json.JsonObject;
import dev.webfx.platform.async.AsyncFunction;

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
