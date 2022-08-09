package dev.webfx.stack.querypush.spi;

import dev.webfx.stack.querypush.PulseArgument;
import dev.webfx.stack.querypush.QueryPushArgument;
import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface QueryPushServiceProvider {

    Future<Object> executeQueryPush(QueryPushArgument argument);

    void executePulse(PulseArgument argument);

}
