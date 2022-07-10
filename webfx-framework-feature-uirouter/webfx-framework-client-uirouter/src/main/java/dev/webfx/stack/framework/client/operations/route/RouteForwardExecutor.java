package dev.webfx.stack.framework.client.operations.route;

import dev.webfx.stack.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.platform.async.Future;

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
