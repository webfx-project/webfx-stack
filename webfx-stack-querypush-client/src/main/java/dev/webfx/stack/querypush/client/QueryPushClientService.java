package dev.webfx.stack.querypush.client;

import dev.webfx.stack.orm.push.client.PushClientService;
import dev.webfx.stack.com.bus.Registration;
import dev.webfx.stack.querypush.QueryPushResult;

import java.util.function.Consumer;

import static dev.webfx.stack.querypush.QueryPushService.QUERY_PUSH_RESULT_LISTENER_CLIENT_SERVICE_ADDRESS;

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