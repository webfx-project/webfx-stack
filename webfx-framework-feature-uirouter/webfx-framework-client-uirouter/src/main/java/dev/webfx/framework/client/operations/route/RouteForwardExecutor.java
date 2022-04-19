package dev.webfx.framework.client.operations.route;

import dev.webfx.platform.client.services.windowhistory.spi.BrowsingHistory;
import dev.webfx.platform.shared.async.Future;

/**
 * @author Bruno Salmon
 */
final class RouteForwardExecutor {

    static Future<Void> executeRequest(RouteForwardRequest rq) {
        return execute(rq.getHistory());
    }

    private static Future<Void> execute(BrowsingHistory history) {
        history.goForward();
        return Future.succeededFuture();
    }
}
