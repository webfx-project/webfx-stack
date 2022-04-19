package dev.webfx.framework.client.operations.route;

import dev.webfx.platform.client.services.windowhistory.spi.BrowsingHistory;
import dev.webfx.platform.shared.async.Future;

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
