package dev.webfx.stack.db.query.buscall;

import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.QueryService;

/**
 * @author Bruno Salmon
 */
public final class ExecuteQueryMethodEndpoint extends AsyncFunctionBusCallEndpoint<QueryArgument, QueryResult> {

    public ExecuteQueryMethodEndpoint() {
        super(QueryServiceBusAddress.EXECUTE_QUERY_METHOD_ADDRESS, arg ->
            QueryService.executeQuery(arg).map(result -> stripMetadataIfRequested(arg, result))
        );
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
