package dev.webfx.stack.db.querypush.buscall;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.db.query.ClientQueryGuard;
import dev.webfx.stack.db.query.ClientScopeToken;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.ServerCallerParameters;
import dev.webfx.stack.db.querypush.QueryPushArgument;
import dev.webfx.stack.db.querypush.QueryPushService;

/**
 * @author Bruno Salmon
 */
public final class ExecuteQueryPushMethodEndpoint extends AsyncFunctionBusCallEndpoint<QueryPushArgument, Object> {

    public ExecuteQueryPushMethodEndpoint() {
        super(QueryPushServiceBusAddress.EXECUTE_QUERY_PUSH_METHOD_ADDRESS, arg -> {
            // A subscription is a query the server then re-runs on every relevant change, so it is checked
            // when it is opened — or re-opened with a different statement, which also arrives here. Control
            // messages (pause, resend, close) carry no query and have nothing to check.
            if (arg != null && arg.getQueryArgument() != null) {
                QueryArgument queryArgument = completeStatement(arg);
                if (queryArgument == null)
                    return Future.failedFuture(ClientQueryGuard.UNCHECKABLE_REFUSED);
                String refusal = ClientQueryGuard.refusalReason(queryArgument);
                if (refusal != null)
                    return Future.failedFuture(refusal);
                // Resolved once, when the subscription is opened, while the principal is still on the thread.
                // A re-fire has no client message and therefore no caller, so the value has to be part of what
                // the subscription stores — and it is, because it goes into the parameters, which is also what
                // keys one subscription apart from another's.
                // The scope is stored with the subscription for the same reason the caller values are: a
                // re-fire has no caller on the thread, and QueryInfo re-runs the argument it was built from.
                QueryArgument resolved;
                try {
                    Object scopeToken = ClientScopeToken.forCaller();
                    QueryArgument scoped = scopeToken == null ? queryArgument
                        : QueryArgument.builder().copy(queryArgument).setScopeToken(scopeToken).build();
                    resolved = ServerCallerParameters.attach(scoped, scopeToken);
                } catch (ClientScopeToken.ScopeUnavailableException | ServerCallerParameters.CallerValueUnavailableException e) {
                    // Refused HERE, before executeQueryPush, and that order is the point: a subscription
                    // opened now would store what was missing in the argument it re-runs, and every re-fire
                    // for the rest of its life would carry it — a missing caller value failing each time, or,
                    // worse, a missing scope quietly re-running unscoped on every change for as long as the
                    // stream lives. Nothing is opened, so the client's retry opens a stream that has both.
                    return Future.failedFuture(e.getMessage());
                }
                if (resolved != arg.getQueryArgument())
                    arg = QueryPushArgument.builder().copy(arg).setQueryArgument(resolved).build();
            }
            return QueryPushService.executeQueryPush(arg);
        });
    }

    /**
     * The query argument with its statement, or null when it has none and none can be found.
     *
     * <p>The Java client leaves out an unchanged statement when only the parameters of an open stream
     * change (ReactiveQueryPushCall), and the stream used to fill it in later, from what it had stored. It is
     * filled in here, before anything else looks at it, because both steps above need it:
     * <ul>
     *   <li>the guard cannot vouch for a statement it cannot see. It threw on the null, and so refused every
     *       such update from 2026-09-18 on;</li>
     *   <li>the caller's own values are attached only to a statement that names a caller term. A
     *       parameters-only update that skipped both would re-run a stored CALLER_PERSON query with only the
     *       parameters the client sent.</li>
     * </ul>
     * The statement is taken only from the caller's OWN stream. A null statement with no stream id, or with
     * someone else's, has nothing to complete it.
     */
    private static QueryArgument completeStatement(QueryPushArgument arg) {
        QueryArgument queryArgument = arg.getQueryArgument();
        if (queryArgument.getStatement() != null)
            return queryArgument;
        String statement = arg.getQueryStreamId() == null ? null : QueryPushService.getStreamStatement(arg.getQueryStreamId());
        if (statement == null)
            return null;
        return QueryArgument.builder().copy(queryArgument).setStatement(statement).build();
    }
}
