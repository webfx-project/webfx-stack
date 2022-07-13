package dev.webfx.stack.routing.uirouter.operations;

import dev.webfx.stack.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.platform.json.JsonObject;
import dev.webfx.stack.async.Future;

/**
 * @author Bruno Salmon
 */
final class RoutePushExecutor {

    static Future<Void> executePushRouteRequest(RoutePushRequest rq) {
        return execute(rq.getRoutePath(), rq.getHistory(), rq.getState());
    }

    private static Future<Void> execute(String routePath, BrowsingHistory history, JsonObject state) {
        if (routePath == null)
            return Future.failedFuture("Route request received with routePath = null!");
        history.push(routePath, state);
        return Future.succeededFuture();
    }
}
