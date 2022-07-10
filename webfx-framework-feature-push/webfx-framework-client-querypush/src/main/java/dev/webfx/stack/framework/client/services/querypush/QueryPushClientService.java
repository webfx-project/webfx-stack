package dev.webfx.stack.framework.client.services.querypush;

import dev.webfx.stack.framework.client.services.push.PushClientService;
import dev.webfx.stack.platform.shared.services.bus.Registration;
import dev.webfx.stack.framework.shared.services.querypush.QueryPushResult;

import java.util.function.Consumer;

import static dev.webfx.stack.framework.shared.services.querypush.QueryPushService.QUERY_PUSH_RESULT_LISTENER_CLIENT_SERVICE_ADDRESS;

/**
 * @author Bruno Salmon
 */
public final class QueryPushClientService {

    // Client side (registering a consumer that will receive the query push results)
    public static Registration registerQueryPushClientConsumer(Consumer<QueryPushResult> javaFunction) {
        return PushClientService.registerPushFunction(QUERY_PUSH_RESULT_LISTENER_CLIENT_SERVICE_ADDRESS, (QueryPushResult qpr) -> {
            javaFunction.accept(qpr);
            return null;
        });
    }

}
