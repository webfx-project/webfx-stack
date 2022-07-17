package dev.webfx.stack.querypush;

import dev.webfx.stack.querypush.spi.QueryPushServiceProvider;
import dev.webfx.stack.async.Future;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class QueryPushService {

    public static final String QUERY_PUSH_SERVICE_ADDRESS = "service/querypush";
    public static final String QUERY_PUSH_RESULT_LISTENER_CLIENT_SERVICE_ADDRESS = "QueryPushResultClientListener";

    public static QueryPushServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(QueryPushServiceProvider.class, () -> ServiceLoader.load(QueryPushServiceProvider.class));
    }

    public static Future<Object> executeQueryPush(QueryPushArgument argument) {
        return getProvider().executeQueryPush(argument);
    }

    public static void executePulse(PulseArgument argument) {
        getProvider().executePulse(argument);
    }

}