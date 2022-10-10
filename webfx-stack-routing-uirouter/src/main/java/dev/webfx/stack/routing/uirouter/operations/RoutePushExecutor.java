package dev.webfx.stack.routing.uirouter.operations;

import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.platform.json.JsonObject;
import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
final class RoutePushExecutor {

    static Future<Void> executePushRouteRequest(RoutePushRequest rq) {
        return execute(rq.getRoutePath(), rq.getHistory(), rq.getState(), rq.isReplace());
    }

    private static Future<Void> execute(String routePath, BrowsingHistory history, JsonObject state, boolean replace) {
        if (routePath == null)
            return Future.failedFuture("Route request received with routePath = null!");
        if (replace)
            history.replace(routePath, state);
        else
            history.push(routePath, state);
        return Future.succeededFuture();
    }
}
