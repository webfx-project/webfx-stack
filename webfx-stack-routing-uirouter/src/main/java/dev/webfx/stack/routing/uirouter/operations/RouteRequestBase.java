package dev.webfx.stack.routing.uirouter.operations;

import dev.webfx.extras.operation.HasOperationExecutor;
import dev.webfx.stack.routing.router.auth.authz.RouteRequest;
import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public abstract class RouteRequestBase<THIS extends RouteRequestBase<THIS>>
        extends RouteRequest
        implements HasOperationExecutor<THIS, Void> {

    private BrowsingHistory history;

    protected RouteRequestBase(BrowsingHistory history) {
        this(null, history);
    }

    protected RouteRequestBase(String routePath, BrowsingHistory history) {
        super(routePath);
        this.history = history;
    }

    public BrowsingHistory getHistory() {
        return history;
    }

    public THIS setHistory(BrowsingHistory history) {
        this.history = history;
        return (THIS) this;
    }

    /* Execute the request without checking at this stage if it is authorized or not, because this is actually the
     * router job to do this checking and to redirect to the login or unauthorized page if not authorized */
    public Future<Void> execute() {
        return getOperationExecutor().apply((THIS) this);
    }
}
