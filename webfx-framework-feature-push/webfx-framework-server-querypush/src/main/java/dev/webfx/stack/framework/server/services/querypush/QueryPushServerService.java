package dev.webfx.stack.framework.server.services.querypush;

import dev.webfx.stack.framework.server.services.push.PushServerService;
import dev.webfx.stack.framework.shared.services.querypush.PulseArgument;
import dev.webfx.stack.framework.shared.services.querypush.QueryPushResult;
import dev.webfx.stack.framework.shared.services.querypush.QueryPushService;
import dev.webfx.stack.db.submitlistener.SubmitListener;
import dev.webfx.stack.db.datascope.DataScope;
import dev.webfx.stack.com.bus.BusService;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.async.Future;

import java.util.Arrays;

import static dev.webfx.stack.framework.shared.services.querypush.QueryPushService.QUERY_PUSH_RESULT_LISTENER_CLIENT_SERVICE_ADDRESS;

/**
 * @author Bruno Salmon
 */
public final class QueryPushServerService {

    // Server side push of a query result to a specific client
    public static <T> Future<T> pushQueryResultToClient(QueryPushResult queryPushResult, Object pushClientId) {
        return PushServerService.callClientService(
                QUERY_PUSH_RESULT_LISTENER_CLIENT_SERVICE_ADDRESS,
                queryPushResult,
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
