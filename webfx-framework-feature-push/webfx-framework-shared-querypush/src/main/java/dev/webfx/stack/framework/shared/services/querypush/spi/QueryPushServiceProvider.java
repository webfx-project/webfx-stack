package dev.webfx.stack.framework.shared.services.querypush.spi;

import dev.webfx.stack.framework.shared.services.querypush.PulseArgument;
import dev.webfx.stack.framework.shared.services.querypush.QueryPushArgument;
import dev.webfx.stack.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface QueryPushServiceProvider {

    Future<Object> executeQueryPush(QueryPushArgument argument);

    void executePulse(PulseArgument argument);

}
