package dev.webfx.framework.shared.services.querypush.spi;

import dev.webfx.framework.shared.services.querypush.PulseArgument;
import dev.webfx.framework.shared.services.querypush.QueryPushArgument;
import dev.webfx.platform.shared.async.Future;

/**
 * @author Bruno Salmon
 */
public interface QueryPushServiceProvider {

    Future<Object> executeQueryPush(QueryPushArgument argument);

    void executePulse(PulseArgument argument);

}
