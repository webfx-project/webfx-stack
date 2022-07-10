package dev.webfx.stack.framework.client.operations.route;

import dev.webfx.stack.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.platform.async.Future;

/**
 * @author Bruno Salmon
 */
final class RouteBackwardExecutor {

    static Future<Void> executeRequest(RouteBackwardRequest rq) {
        return execute(rq.getHistory());
    }

    private static Future<Void> execute(BrowsingHistory history) {
        history.goBack();
        return Future.succeededFuture();
    }
}
