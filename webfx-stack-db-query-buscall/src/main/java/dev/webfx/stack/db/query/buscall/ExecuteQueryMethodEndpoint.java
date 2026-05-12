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

    /** Strip columnNames and entityMapping from the result when the client has them cached. */
    static QueryResult stripMetadataIfRequested(QueryArgument arg, QueryResult result) {
        if (!arg.isSendMetadata() && result != null) {
            QueryResult stripped = new QueryResult(result.getRowCount(), result.getColumnCount(), result.getValues(), null);
            stripped.setVersionNumber(result.getVersionNumber());
            // entityMapping intentionally NOT copied — client has it cached
            return stripped;
        }
        return result;
    }
}
