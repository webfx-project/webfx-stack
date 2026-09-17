package dev.webfx.stack.db.querypush.buscall;

import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.db.querypush.DatabaseHealthMonitorInfo;
import dev.webfx.stack.db.querypush.QueryPushService;

/**
 * Bus endpoint exposing the read-only database health snapshot (connections + non-idle/long-running/
 * blocking queries from {@code pg_stat_activity}) to administration consoles. Fetched on demand when
 * the /monitor "Database" drill-down opens — not on the regular monitor poll, since it queries the DB.
 * The provider fails the future when the caller isn't a logged-in back-office user.
 *
 * @author Bruno Salmon
 */
public final class GetDatabaseHealthInfoMethodEndpoint extends AsyncFunctionBusCallEndpoint<Object, DatabaseHealthMonitorInfo> {

    public GetDatabaseHealthInfoMethodEndpoint() {
        super(QueryPushServiceBusAddress.GET_DATABASE_HEALTH_INFO_METHOD_ADDRESS,
            ignoredArgument -> QueryPushService.getDatabaseHealthInfo());
    }
}
