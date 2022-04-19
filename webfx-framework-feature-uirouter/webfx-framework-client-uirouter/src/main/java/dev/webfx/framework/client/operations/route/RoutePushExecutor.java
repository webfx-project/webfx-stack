package dev.webfx.framework.client.operations.route;

import dev.webfx.platform.client.services.windowhistory.spi.BrowsingHistory;
import dev.webfx.platform.shared.services.json.JsonObject;
import dev.webfx.platform.shared.async.Future;

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
