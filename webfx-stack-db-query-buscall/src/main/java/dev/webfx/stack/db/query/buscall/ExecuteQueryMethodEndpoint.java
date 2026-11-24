package dev.webfx.stack.db.query.buscall;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.db.query.ClientQueryGuard;
import dev.webfx.stack.db.query.ClientScopeToken;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.QueryService;
import dev.webfx.stack.db.query.ServerCallerParameters;

/**
 * @author Bruno Salmon
 */
public final class ExecuteQueryMethodEndpoint extends AsyncFunctionBusCallEndpoint<QueryArgument, QueryResult> {

    public ExecuteQueryMethodEndpoint() {
        super(QueryServiceBusAddress.EXECUTE_QUERY_METHOD_ADDRESS, arg -> {
            // Every query arriving here was sent by a client, so it passes the client guard before it runs —
            // see ClientQueryGuard for what that refuses, and why server code never comes through here.
            String refusal = ClientQueryGuard.refusalReason(arg);
            if (refusal != null)
                return Future.failedFuture(refusal);
            // Both resolved HERE because the principal is on this thread and will not be by the time the
            // query is bound — see ServerCallerParameters. After the guard, so a refused query never
            // resolves a caller, and before execution, so the terms have values by the time they compile.
            // The scope is asked at the same door for the same reason, and because this endpoint is where a
            // client's own statement arrives while an endpoint's internal lookup never passes through it. It
            // rides on the argument because a push re-fire has no thread state left to read it from, and it
            // joins the argument's equality so two scopes never share one compiled statement or one
            // subscription.
            QueryArgument resolved;
            try {
                Object scopeToken = ClientScopeToken.forCaller();
                QueryArgument scoped = scopeToken == null ? arg
                    : QueryArgument.builder().copy(arg).setScopeToken(scopeToken).build();
                resolved = ServerCallerParameters.attach(scoped, scopeToken);
            } catch (ClientScopeToken.ScopeUnavailableException | ServerCallerParameters.CallerValueUnavailableException e) {
                // Two ways one read arrives unanswerable, refused the same way. Either the statement needs a
                // value about this caller that the server does not have yet, and running it anyway would bind
                // null and answer with an empty result that looks exactly like "you may read nothing"; or a
                // scope is enforced and this caller's could not be resolved, and running it anyway would
                // compile with no scope condition at all and answer with everyone's rows. Refused instead,
                // retriably: the lookup that was missing is started by the miss, so the client's retry finds
                // it — see CallerReadScope in modality-crm.
                return Future.failedFuture(e.getMessage());
            }
            return QueryService.executeQuery(resolved).map(result -> stripMetadataIfRequested(arg, result));
        });
    }

    /**
     * Strip SQL column names from the result when the client has them cached, but keep
     * entityMapping on the wire. The React client has no DQL runtime so the wire
     * entityMapping is its only source of DQL field paths; relying on the client cache
     * here is racy (cold subscribes / reconnects can hit an empty cache and then decode
     * every subsequent row with positional `col0`/`col1` keys for the subscription's
     * lifetime). The mapping is small relative to the row payload, so always sending it
     * is the cheap correct fix.
     */
    static QueryResult stripMetadataIfRequested(QueryArgument arg, QueryResult result) {
        if (!arg.isSendMetadata() && result != null) {
            QueryResult stripped = new QueryResult(result.getRowCount(), result.getColumnCount(), result.getValues(), null);
            stripped.setVersionNumber(result.getVersionNumber());
            stripped.setEntityMapping(result.getEntityMapping());
            stripped.setCallSeq(result.getCallSeq());
            return stripped;
        }
        return result;
    }
}
