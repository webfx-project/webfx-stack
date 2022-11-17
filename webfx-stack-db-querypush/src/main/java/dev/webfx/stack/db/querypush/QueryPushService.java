package dev.webfx.stack.db.querypush;

import dev.webfx.stack.db.querypush.spi.QueryPushServiceProvider;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class QueryPushService {

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
