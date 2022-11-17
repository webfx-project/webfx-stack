package dev.webfx.stack.db.querypush.client;

import dev.webfx.stack.db.querypush.QueryPushResult;
import dev.webfx.stack.db.querypush.buscall.QueryPushListenerBusAddress;
import dev.webfx.stack.orm.push.client.PushClientService;
import dev.webfx.stack.com.bus.Registration;

import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public final class QueryPushClientService {

    // Client side (registering a consumer that will receive the query push results)
    public static Registration registerQueryPushClientConsumer(Consumer<QueryPushResult> javaFunction) {
        return PushClientService.registerPushFunction(QueryPushListenerBusAddress.QUERY_PUSH_RESULT_CLIENT_LISTENER_ADDRESS, (QueryPushResult qpr) -> {
            javaFunction.accept(qpr);
            return null;
        });
    }

}
