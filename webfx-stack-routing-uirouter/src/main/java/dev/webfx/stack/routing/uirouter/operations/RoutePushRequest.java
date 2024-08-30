package dev.webfx.stack.routing.uirouter.operations;

import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.async.AsyncFunction;

/**
 * @author Bruno Salmon
 */
public class RoutePushRequest extends RouteRequestBase<RoutePushRequest> {

    private ReadOnlyAstObject state;
    private boolean replace; // flag asking to replace the current url instead of pushing a new entry in the browser history

    public RoutePushRequest(String routePath, BrowsingHistory browsingHistory) {
        this(routePath, browsingHistory, null);
    }

    public RoutePushRequest(String routePath, BrowsingHistory history, ReadOnlyAstObject state) {
        super(routePath, history);
        this.state = state;
    }

    public ReadOnlyAstObject getState() {
        return state;
    }

    public RoutePushRequest setState(ReadOnlyAstObject state) {
        this.state = state;
        return this;
    }

    public boolean isReplace() {
        return replace;
    }

    public void setReplace(boolean replace) {
        this.replace = replace;
    }

    @Override
    public AsyncFunction<RoutePushRequest, Void> getOperationExecutor() {
        return RoutePushExecutor::executePushRouteRequest;
    }
}
