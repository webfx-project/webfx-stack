package dev.webfx.stack.db.querypush.server;

import dev.webfx.stack.com.bus.DeliveryOptions;
import dev.webfx.stack.db.querypush.PulseArgument;
import dev.webfx.stack.db.querypush.QueryPushResult;
import dev.webfx.stack.db.querypush.QueryPushService;
import dev.webfx.stack.push.server.PushServerService;
import dev.webfx.stack.db.submit.listener.SubmitListener;
import dev.webfx.stack.db.datascope.DataScope;
import dev.webfx.stack.com.bus.BusService;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.platform.async.Future;

import java.util.Arrays;

import static dev.webfx.stack.db.querypush.buscall.QueryPushListenerBusAddress.QUERY_PUSH_RESULT_CLIENT_LISTENER_ADDRESS;

/**
 * @author Bruno Salmon
 */
public final class QueryPushServerService {

    // Server side push of a query result to a specific client
    public static <T> Future<T> pushQueryResultToClient(QueryPushResult queryPushResult, Object pushClientId) {
        return PushServerService.push(
                QUERY_PUSH_RESULT_CLIENT_LISTENER_ADDRESS,
                queryPushResult,
                new DeliveryOptions(),
                BusService.bus(),
                pushClientId
        );
    }

    public static class ProvidedSubmitListener implements SubmitListener {

        @Override
        public void onSuccessfulSubmit(SubmitArgument... arguments) {
            if (arguments != null && arguments.length > 0)
                QueryPushService.executePulse(
                        PulseArgument.createToRefreshAllQueriesImpactedByDataScope(
                                arguments[0].getDataSourceId(),
                                DataScope.concat(Arrays.stream(arguments).map(SubmitArgument::getDataScope).toArray(DataScope[]::new))
                        )
                );
        }

    }
}
